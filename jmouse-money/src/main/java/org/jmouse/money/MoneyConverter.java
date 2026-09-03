package org.jmouse.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

/**
 * 🔄 One currency into another, through the pivot the rates are quoted against.
 *
 * <h3>⚠️ Divide LAST, and only once</h3>
 *
 * <p>The obvious implementation converts to the pivot, then converts out of it — two divisions, each
 * rounding, and the error compounds. This one multiplies by the source rate and divides by the target
 * rate as a single expression, with the division carried out once at the stated scale:</p>
 *
 * <pre>{@code amount × rateToPivot(source) ÷ rateToPivot(target)}</pre>
 *
 * <p>A stock total is exactly the sort of figure somebody quotes somewhere expensive, so the difference
 * between one rounding and two is worth the sentence it takes to explain.</p>
 *
 * <h3>⚠️ Converting a currency to itself returns it UNTOUCHED</h3>
 *
 * <p>Not "multiplied by one and re-scaled". Rescaling would quietly change a value's precision on a
 * call that was supposed to be a no-op, and the caller has no way to tell that it happened.</p>
 *
 * <h3>⚠️ Empty is an answer, and every caller must treat it as one</h3>
 *
 * <p>A missing rate comes back empty rather than as a zero or as the unconverted amount. Both of those
 * fold something unknown into a total that then looks complete; empty is the only outcome a reader can
 * be told about.</p>
 */
public final class MoneyConverter {

    /**
     * 📏 Four decimal places, which is a working scale rather than a display one.
     *
     * <p>A caller totalling money asks for two; this is the default because a converted figure is
     * usually added to others before anybody looks at it, and rounding to display precision first is
     * how a total ends up a few units away from the sum of what it lists.</p>
     */
    public static final int DEFAULT_SCALE = 4;

    /** 📏 Half-up, which is what a person doing this on paper does. */
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    private final ExchangeRates rates;

    public MoneyConverter(ExchangeRates rates) {
        this.rates = Objects.requireNonNull(rates, "A converter needs somewhere to read rates from");
    }

    /**
     * 🔄 An amount, in another currency, at {@link #DEFAULT_SCALE}.
     *
     * @param source what is being converted
     * @param target what it is being converted into
     * @return the converted amount, or empty where either currency has no rate
     */
    public Optional<Money> convert(Money source, CurrencyCode target) {
        return convert(source, target, DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * 🔄 An amount, in another currency, at a stated precision.
     *
     * @param source   what is being converted
     * @param target   what it is being converted into
     * @param scale    decimal places for the single division
     * @param rounding how that division rounds
     * @return the converted amount, or empty where either currency has no rate
     */
    public Optional<Money> convert(Money source, CurrencyCode target, int scale, RoundingMode rounding) {
        if (source.currency().equals(target)) {
            return Optional.of(source);
        }

        Optional<BigDecimal> sourceRate = rateOf(source.currency());
        Optional<BigDecimal> targetRate = rateOf(target);

        if (sourceRate.isEmpty() || targetRate.isEmpty()) {
            return Optional.empty();
        }

        // ⚠️ A rate of zero would be a division by zero, and a negative one is nonsense that would flip
        // every sign it touched. Both mean the stored rate is wrong rather than that this call is, so
        // they are reported the same way a missing rate is: unconvertible.
        if (targetRate.get().signum() <= 0 || sourceRate.get().signum() <= 0) {
            return Optional.empty();
        }

        BigDecimal converted = source.amount()
                .multiply(sourceRate.get())
                .divide(targetRate.get(), scale, rounding);

        return Optional.of(new Money(converted, target));
    }

    /** 💱 The currency every conversion here routes through. */
    public CurrencyCode pivot() {
        return rates.pivot();
    }

    /**
     * 📈 A currency's rate, with the pivot's own answered here rather than looked up.
     *
     * <p>⚠️ Answered before the store is asked, so that neither a feed which omits the pivot nor a table
     * which stores it can decide what one-to-one means. See {@link ExchangeRates}.</p>
     */
    private Optional<BigDecimal> rateOf(CurrencyCode currency) {
        if (currency.equals(rates.pivot())) {
            return Optional.of(BigDecimal.ONE);
        }

        return rates.rateToPivot(currency);
    }
}
