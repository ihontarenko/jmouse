package org.jmouse.money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * 📈 Where rates come from, as far as this library is concerned.
 *
 * <h3>⚠️ The pivot is ASKED FOR, never assumed</h3>
 *
 * <p>Every rate here is quoted against one currency — the pivot — because that is how published feeds
 * work: the National Bank of Ukraine quotes the hryvnia, the European Central Bank quotes the euro. A
 * converter that hardwired one of them could not be given the other, and hardwiring the hryvnia is
 * precisely what makes an existing implementation in this workspace impossible to lift into a library.
 * So the pivot is a question, and {@link MoneyConverter} routes through whatever the answer is.</p>
 *
 * <h3>⚠️ The pivot itself is never a row</h3>
 *
 * <p>Its rate against itself is one, by definition. An implementation may return {@code ONE} for it and
 * may equally return empty; {@link MoneyConverter} answers the question before asking, so that neither
 * a feed that omits it nor a table that stores it can get it wrong. Storing it would invite somebody to
 * edit it, and a pivot rate of anything but one silently rescales every conversion at once.</p>
 *
 * <h3>⚠️ A missing rate is an ORDINARY answer</h3>
 *
 * <p>Nobody has synced yet; the feed does not carry that currency; somebody added a price in a currency
 * their bank does not publish. All three are normal, all three are empty, and every one of them has to
 * reach a reader as <em>this part could not be converted</em> rather than as a zero folded into a total.</p>
 */
public interface ExchangeRates {

    /** 💱 The currency every rate here is quoted against. */
    CurrencyCode pivot();

    /**
     * 📈 How many units of the {@link #pivot()} one unit of this currency is worth.
     *
     * @param currency the currency being asked about
     * @return the rate, or empty where none is held — which is an answer, not a failure
     */
    Optional<BigDecimal> rateToPivot(CurrencyCode currency);

    /**
     * 🕒 When these rates were last refreshed.
     *
     * <p>⚠️ Exists so a screen can say so, and that is not decoration: a converted total whose rates
     * are three months old is as misleading as no total at all, and the age is the only thing that
     * distinguishes them. Empty where nothing has ever been loaded.</p>
     */
    Optional<Instant> lastUpdated();
}
