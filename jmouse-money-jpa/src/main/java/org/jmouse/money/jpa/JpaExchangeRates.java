package org.jmouse.money.jpa;

import org.jmouse.money.CurrencyCode;
import org.jmouse.money.ExchangeRates;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * 📈 The rate seam, answered from rows.
 *
 * <p>⚠️ <strong>The pivot is constructor state, and is not read from the table.</strong> Asking the
 * rows what they are quoted against would let a stale row decide what every conversion means: swap the
 * feed, forget to clear the old rows, and the converter silently starts routing through whatever the
 * table happened to hold. The pivot is a decision, so it is supplied by whoever made it — and
 * {@code ExchangeRateRegistry#pivots()} is how the service checks that the rows agree.</p>
 *
 * <p>⚠️ <strong>It reads. It does not fetch, schedule or decide.</strong> Everything that talks to a
 * bank, runs on a timer, or knows that a manual rate survives a sync lives above this class.</p>
 */
public final class JpaExchangeRates implements ExchangeRates {

    private final ExchangeRateRegistry registry;
    private final CurrencyCode         pivot;

    public JpaExchangeRates(ExchangeRateRegistry registry, CurrencyCode pivot) {
        this.registry = Objects.requireNonNull(registry, "A rate reader needs a registry");
        this.pivot    = Objects.requireNonNull(pivot, "A rate reader needs to know what the rates are quoted against");
    }

    @Override
    public CurrencyCode pivot() {
        return pivot;
    }

    @Override
    public Optional<BigDecimal> rateToPivot(CurrencyCode currency) {
        return registry.find(currency.code(), pivot.code()).map(ExchangeRate::getRateToPivot);
    }

    @Override
    public Optional<Instant> lastUpdated() {
        return registry.lastUpdated(pivot.code());
    }
}
