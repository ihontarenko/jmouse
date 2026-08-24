package org.jmouse.query.schema;

/**
 * What kind of value an attribute holds.
 *
 * <p>Deliberately small. This is not a type system — it is the handful of distinctions a compiler has to
 * make: whether a comparison is ordered, whether a value needs quoting, whether {@code contains} means
 * anything. Everything finer belongs to the product that owns the data.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum QueryType {

    /** Words. Ordered comparison is legal but almost never meant — see {@link #isOrdered()}. */
    TEXT(false),

    /** Whole and fractional numbers alike; the distinction is the data source's, not the language's. */
    NUMBER(true),

    /** True or false, and nothing in between. */
    BOOLEAN(false),

    /** A moment or a day. Ordered, which is most of what anybody asks of one. */
    TEMPORAL(true),

    /**
     * ⚠️ A value whose type nobody knows — the schemaless case.
     *
     * <p>This is what an attribute is when it lives in a bag of text and the schema declines to promise
     * anything about it. It is the reason the converter pipes exist, and the reason
     * {@link QueryAttribute} refuses an ordered comparison over one without a converter.</p>
     */
    UNKNOWN(false);

    private final boolean ordered;

    QueryType(boolean ordered) {
        this.ordered = ordered;
    }

    /**
     * Whether {@code &lt;} and {@code &gt;} mean what a reader expects.
     *
     * <p>⚠️ {@link #TEXT} answers {@code false}, and that is not an oversight. Text <em>can</em> be
     * ordered — lexicographically — and that ordering is almost never the one somebody comparing a
     * quantity has in mind. Saying "no" here is what makes a converter compulsory rather than optional.</p>
     *
     * @return {@code true} when an ordered comparison is meaningful without a converter
     */
    public boolean isOrdered() {
        return ordered;
    }
}
