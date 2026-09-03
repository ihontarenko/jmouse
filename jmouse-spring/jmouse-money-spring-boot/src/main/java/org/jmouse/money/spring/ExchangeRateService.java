package org.jmouse.money.spring;

import org.jmouse.money.CurrencyCode;
import org.jmouse.money.jpa.ExchangeRate;
import org.jmouse.money.jpa.ExchangeRateRegistry;
import org.jmouse.money.jpa.ExchangeRateSource;
import org.jmouse.money.spring.provider.ExchangeRateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 💱 Everything that decides what a rate row may become.
 *
 * <p>The registry beneath this puts rows in and takes them out and knows nothing else; every rule about
 * <em>which</em> rows may be written lives here, in one place, where the reasons can sit next to each
 * other.</p>
 */
public class ExchangeRateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRateService.class);

    private final ExchangeRateRegistry registry;
    private final ExchangeRateProvider provider;
    private final CurrencyCode         pivot;

    public ExchangeRateService(ExchangeRateRegistry registry, ExchangeRateProvider provider, CurrencyCode pivot) {
        this.registry = Objects.requireNonNull(registry, "The rate service needs a registry");
        this.provider = Objects.requireNonNull(provider, "The rate service needs a provider");
        this.pivot    = Objects.requireNonNull(pivot, "The rate service needs to know what rates are quoted against");
    }

    /**
     * 📡 Ask the feed and write what it says.
     *
     * <p>⚠️ <strong>A {@link ExchangeRateSource#MANUAL} row is never touched.</strong> Somebody pinned it
     * on purpose, and {@link #resetToProvider(String)} is the only way back — a sync that quietly undid a
     * deliberate decision would be worse than no sync.</p>
     *
     * @return what happened, in numbers a screen can report
     */
    @Transactional
    public SyncOutcome sync() {
        refuseAMismatchedPivot();
        refuseAMixedTable();

        Map<CurrencyCode, BigDecimal> published = provider.fetchRatesToPivot();

        int written = 0;
        int pinned  = 0;

        for (Map.Entry<CurrencyCode, BigDecimal> rate : published.entrySet()) {
            String currency = rate.getKey().code();

            // ⚠️ The pivot is never a row: its rate against itself is one by definition. A feed that sent
            // it anyway would otherwise put an editable 1.000000 in the table for somebody to change.
            if (rate.getKey().equals(pivot)) {
                continue;
            }

            ExchangeRate existing = registry.find(currency).orElse(null);

            if (existing != null && existing.getSource() == ExchangeRateSource.MANUAL) {
                pinned++;
                continue;
            }

            if (existing == null) {
                registry.save(new ExchangeRate(currency, pivot.code(), rate.getValue(), ExchangeRateSource.PROVIDER));
            } else {
                existing.setRateToPivot(rate.getValue());
                existing.setPivot(pivot.code());
                existing.setSource(ExchangeRateSource.PROVIDER);
                registry.save(existing);
            }

            written++;
        }

        LOGGER.info("💱 Synced {} rate(s) against {} from '{}'; {} left alone as manual",
                    written, pivot, provider.name(), pinned);

        return new SyncOutcome(provider.name(), pivot.code(), published.size(), written, pinned);
    }

    /**
     * ✍️ Pin a rate by hand.
     *
     * <p>⚠️ It stops tracking the feed permanently, until {@link #resetToProvider(String)} is called.
     * That is the point and it is also the trap, so anything displaying these rows must mark them.</p>
     *
     * @throws IllegalArgumentException where the currency is not one this library reads, or the rate is
     *                                  not positive — a zero rate is a division by zero waiting to happen
     *                                  and a negative one flips every sign it touches
     */
    @Transactional
    public ExchangeRate setManualRate(String currency, BigDecimal rate) {
        CurrencyCode recognised = CurrencyCode.required(currency);

        if (rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException("A rate has to be greater than zero, and this one is: " + rate);
        }

        if (recognised.equals(pivot)) {
            throw new IllegalArgumentException(
                    pivot + " is what every rate here is quoted against — its own rate is one, and is not stored");
        }

        ExchangeRate existing = registry.find(recognised.code()).orElse(null);

        if (existing == null) {
            return registry.save(new ExchangeRate(recognised.code(), pivot.code(), rate, ExchangeRateSource.MANUAL));
        }

        existing.setRateToPivot(rate);
        existing.setPivot(pivot.code());
        existing.setSource(ExchangeRateSource.MANUAL);

        return registry.save(existing);
    }

    /**
     * ↩️ Hand one currency back to the feed.
     *
     * @throws IllegalStateException where the feed does not publish that currency, because marking a row
     *                               as provider-controlled when no provider controls it leaves a rate
     *                               that will never move again and no longer says so
     */
    @Transactional
    public ExchangeRate resetToProvider(String currency) {
        refuseAMismatchedPivot();

        CurrencyCode recognised = CurrencyCode.required(currency);
        BigDecimal   published  = provider.fetchRatesToPivot().get(recognised);

        if (published == null) {
            throw new IllegalStateException(
                    "'" + provider.name() + "' publishes no rate for " + recognised + ", so it cannot take it back");
        }

        ExchangeRate existing = registry.find(recognised.code()).orElse(null);

        if (existing == null) {
            return registry.save(new ExchangeRate(recognised.code(), pivot.code(), published,
                                                  ExchangeRateSource.PROVIDER));
        }

        existing.setRateToPivot(published);
        existing.setPivot(pivot.code());
        existing.setSource(ExchangeRateSource.PROVIDER);

        return registry.save(existing);
    }

    /** 📋 Every rate held, for a screen. Includes rows left behind by a previous pivot, deliberately. */
    @Transactional(readOnly = true)
    public List<ExchangeRate> list() {
        return registry.all();
    }

    /** 💱 What every rate here is quoted against. */
    public CurrencyCode pivot() {
        return pivot;
    }

    /**
     * 🏷️ Which feed these rates come from.
     *
     * <p>⚠️ For a screen to name on its Sync button, and it is worth naming: pressing something called
     * "Sync" without knowing what it is about to contact is how somebody discovers on a metered
     * connection, or on an air-gapped one, that it reaches the public internet.</p>
     */
    public String provider() {
        return provider.name();
    }

    /**
     * ⚠️ A provider quoting something else is refused rather than converted.
     *
     * <p>Converting the feed into another pivot would need rates this code does not have — that is what
     * it is fetching. So the only honest outcomes are to use what the feed publishes or to refuse, and
     * refusing names both sides so the misconfiguration is obvious.</p>
     */
    private void refuseAMismatchedPivot() {
        if (!provider.pivot().equals(pivot)) {
            throw new IllegalStateException(
                    "'" + provider.name() + "' quotes against " + provider.pivot() + ", but this installation stores "
                    + "rates against " + pivot + " — set jmouse.money.pivot to " + provider.pivot()
                    + " or configure a feed that publishes " + pivot);
        }
    }

    /**
     * ⚠️ Rows against two pivots at once are refused before anything is written.
     *
     * <p>It happens when a pivot is changed without clearing what the previous feed wrote. Nothing else
     * notices: every row is individually plausible and only the set of them is wrong, so a total built on
     * such a table is wrong by an amount nobody can work out afterwards.</p>
     */
    private void refuseAMixedTable() {
        Set<String> stored = registry.pivots();

        if (stored.size() > 1 || (stored.size() == 1 && !stored.contains(pivot.code()))) {
            throw new IllegalStateException(
                    "The rate table holds rows quoted against " + stored + " while this installation stores against "
                    + pivot + " — clear the old rows before syncing, or nothing here means what it says");
        }
    }

    /**
     * 📊 What one sync did.
     *
     * @param provider    which feed answered
     * @param pivot       what the rates are quoted against
     * @param published   how many rates the feed carried
     * @param written     how many rows were written
     * @param leftAsManual how many were skipped because somebody had pinned them
     */
    public record SyncOutcome(String provider, String pivot, int published, int written, int leftAsManual) {}
}
