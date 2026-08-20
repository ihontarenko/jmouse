package org.jmouse.access.spi;

import org.jmouse.access.ConsumptionKey;

/**
 * How much has been used — the read side of a quota, and the only side a decision may touch.
 *
 * <h2>⚠️ Read-only, and not because writing was inconvenient</h2>
 *
 * <p>This is what a condition function calls, which means it runs on the decision path — where the
 * permission resolution is memoised per {@code (subject, scope chain)} and one answer serves a whole
 * page of rows. A function that <em>spent</em> a quota here would spend it once, or twenty-five times,
 * depending on whether the cache happened to miss. That is not a number anybody can predict, and it
 * would appear on an invoice.
 *
 * <p>So recording stays where it always was: a write after the transaction that consumed something has
 * committed, made by the product that knows what it consumed. Check and spend are separate acts, in
 * that order, and the type system says so.
 *
 * <h2>Answering nothing is answering zero</h2>
 *
 * <p>A window nobody has written to has no row, and that is not an error — it is a period that has not
 * been used yet. A store that threw for a missing counter would make the first request of every month
 * fail.
 */
public interface ConsumptionCounters {

    /**
     * What one subject has used of one meter in one window.
     *
     * @param key what is counted, for whom, when
     * @return the amount consumed, or zero where nothing has been recorded
     */
    long consumed(ConsumptionKey key);

    /** A store that has never recorded anything, for a product that meters nothing. */
    static ConsumptionCounters empty() {
        return key -> 0L;
    }
}
