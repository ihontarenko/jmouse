package org.jmouse.access.spi;

import org.jmouse.access.CardinalityKey;

/**
 * How many of something exist right now — the port behind {@code count(kind)} in a policy condition.
 *
 * <p>The product implements it over whatever already answers the question; the library never opens a
 * repository of its own.
 *
 * <h2>⚠️ This is not {@link ConsumptionCounters}, and the difference is the dangerous part</h2>
 *
 * <table>
 *   <tr><th></th><th>{@code consumed()}</th><th>{@code count()}</th></tr>
 *   <tr><td>answers</td><td>how much was spent</td><td>how many exist</td></tr>
 *   <tr><td>goes down</td><td>never — a window rolls over instead</td><td><strong>yes</strong>, when something is deleted</td></tr>
 *   <tr><td>costs</td><td>one indexed row</td><td>⚠️ a query, per decision</td></tr>
 * </table>
 *
 * <p>⚠️ <strong>A count that goes down is a quota somebody can farm</strong>: create ten, delete one,
 * create one. That is correct behaviour for a seat limit — you freed a seat, you may fill it — and it is
 * <em>wrong</em> for a spend limit. So a rule guarding money or tokens wants
 * {@link ConsumptionCounters}; a rule guarding <em>how many rows may exist</em> wants this. Nobody
 * should reach for {@code count('ai-request')}.
 *
 * <h2>⚠️ It has to be cheap, and that is part of the contract</h2>
 *
 * <p>A consumption read is one indexed row. A cardinality count is a {@code SELECT COUNT(*)} over a
 * product table, run <strong>inside permission resolution</strong>, on the last axis of every request.
 *
 * <p>So: <strong>answer from an index, with no join</strong>. A product that cannot make it cheap should
 * maintain a counter row and read that instead — this port is exactly the seam where that swap happens
 * without a single rule changing.
 *
 * <h2>⚠️ Read, never write</h2>
 *
 * <p>Same rule as every other condition function. Permission resolution is memoised per
 * {@code (subject, scope chain)} and one answer serves a page of rows, so anything with a side effect
 * would have it an unpredictable number of times.
 */
public interface CardinalityCounters {

    /**
     * How many of that kind exist at that place.
     *
     * @param key what to count and where
     * @return the count, never negative
     */
    long count(CardinalityKey key);

    /**
     * The installation that counts nothing.
     *
     * <p>⚠️ It answers <strong>0</strong>, which in the shape {@code count(…) >= allowance(…)} means the
     * limit is never reached and the rule never refuses. That is the right default for a port nobody has
     * implemented — the alternative, refusing everything, would break an installation that merely has not
     * adopted the feature. The load-time check on declared kinds is what stops a rule silently relying on
     * it.
     */
    static CardinalityCounters empty() {
        return key -> 0L;
    }
}
