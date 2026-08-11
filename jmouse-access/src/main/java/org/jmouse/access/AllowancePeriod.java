package org.jmouse.access;

/**
 * The window a consumed quantity is counted over.
 *
 * <p>Only meaningful on something metered by <em>consumption</em>. A standing count — how many of a
 * thing exist right now — is answered by counting what exists, so it has no window and cannot drift;
 * a consumed one has to be attributed to a period, because nothing existing can be counted to find
 * out how much was written last month.
 *
 * <p>⚠️ <strong>{@link #EVER} is a period, not the absence of one.</strong> A lifetime allowance is
 * still consumed and still counted; what it is not is <em>reset</em>. The absence of a ceiling is
 * {@link Allowance#unlimited()}, which is a different fact about a different column, and conflating
 * the two is how "unlimited" ends up meaning "a very large number until next month".
 *
 * <p>Deliberately four. A period a product cannot explain in a sentence on an invoice is a period
 * nobody can reconcile against one.
 */
public enum AllowancePeriod {

    /** Counted per calendar day. */
    DAY,

    /** Counted per calendar month — the ordinary billing rhythm. */
    MONTH,

    /** Counted per calendar year. */
    YEAR,

    /** Counted once, for the life of the grant. Never reset. */
    EVER
}
