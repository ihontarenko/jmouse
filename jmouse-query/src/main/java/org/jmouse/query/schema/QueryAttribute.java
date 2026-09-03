package org.jmouse.query.schema;

/**
 * One thing a query may filter, sort or return — what it is called, what a person reads, what the store
 * calls it, what it holds, and how it is reached.
 *
 * @param name   how it is written in a query — {@code entry[component_name]}, {@code issue.assignee}
 * @param label  what a person reads instead — {@code "Lines with no source"}, or {@code null}
 * @param source what the store calls it — {@code component_name}, {@code assignee_id}
 * @param type   what kind of value it holds
 * @param access how a compiler gets at it
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record QueryAttribute(String name, String label, String source, QueryType type, Access access) {

    /**
     * An attribute that has no separate label — a query's name and a person's are the same word.
     *
     * <p>⚠️ Kept so that adding {@link #label} renamed nothing. Every product and every smoke test that
     * built a four-argument attribute still compiles, and reads exactly as it did.</p>
     */
    public QueryAttribute(String name, String source, QueryType type, Access access) {
        this(name, null, source, type, access);
    }

    /**
     * An attribute a query writes exactly as the store spells it.
     *
     * @param name   the name, in both senses
     * @param type   what kind of value it holds
     * @param access how it is reached
     */
    public QueryAttribute(String name, QueryType type, Access access) {
        this(name, null, name, type, access);
    }

    /**
     * What to show somebody who is building a query — the label where one was declared, else the name.
     *
     * <h2>⚠️ Why a label belongs to the SCHEMA and not to whoever draws the builder</h2>
     *
     * <p>A product that keeps its labels beside its screens keeps the same list of attribute names
     * twice: once where the source is declared, once in a map of pretty words — and nothing holds the
     * two together. Renaming an attribute in the declaration then leaves the map keyed on a name that no
     * longer exists, the lookup misses, and the builder quietly shows the raw name. Nothing fails; the
     * screen is simply worse, in a way only somebody who remembers the old label would notice.</p>
     *
     * <p>⚠️ <strong>A label is never queryable.</strong> It is what a person reads, and a query still
     * writes {@link #name}. Making it a second spelling would mean two ways to write one condition, and
     * a saved query that stops parsing the day somebody improves the wording.</p>
     *
     * @return the label, or the name where none was declared — never {@code null}
     */
    public String readableName() {
        return label == null || label.isBlank() ? name : label;
    }

    /**
     * How a compiler reaches an attribute.
     *
     * <p>⚠️ <strong>This is a seam, not a detail.</strong> Two products differ here in kind rather than
     * in degree: one keeps every value as a row in a bag, so each attribute costs a join and an alias;
     * the other has real columns, so the same expression compiles to a plain {@code WHERE} with no join
     * at all. One compiler serves both only if the difference is <em>declared by the product</em>
     * instead of branched on inside the library — a branch would have to know which product it was
     * serving, and there is no honest way for it to find out.</p>
     */
    public enum Access {

        /** A real column. Its type comes from the schema, so no converter is needed to compare it. */
        COLUMN,

        /**
         * A row in a bag of values, reached by a join.
         *
         * <p>⚠️ Every distinct attribute needs its <strong>own</strong> alias. One row of a bag cannot be
         * both the name and the quantity, so collapsing two accesses into one join silently returns
         * nothing where an {@code and} was meant — and it looks right on small data.</p>
         */
        BAG,

        /**
         * A column of a row one hop away — {@code issue.status.category}, normalised into its own table.
         *
         * <p>⚠️ The opposite aliasing rule to a bag's: two joined attributes from the same table are two
         * columns of <strong>one</strong> row, so they share a single join. Giving each its own would
         * join the same table twice for no reason and, where the join is anything but a key lookup,
         * multiply the rows.</p>
         */
        JOINED,

        /**
         * Many rows per row, with no single value — labels, tags, watchers.
         *
         * <p>⚠️ <strong>It has no expression, and that is the point.</strong> A collection can only be
         * asked a question — {@code is hasAny([…])} — which compiles to {@code EXISTS}. Reaching it as a
         * value would mean joining it, and a row with three labels would then come back three times,
         * quietly making every count over that result wrong.</p>
         */
        COLLECTION
    }

    /**
     * Whether an ordered comparison of this attribute needs a converter first.
     *
     * <p>⚠️ <strong>The single rule this whole schema exists to enforce.</strong> A bag holds every value
     * as text, and as text {@code "900" > "1000"} is <em>true</em> — because {@code "9" > "1"}. So a
     * query written {@code entry[resistance] > 3300} answers wrongly on every row, forever, with no
     * error anywhere. Refused, it costs somebody four characters.</p>
     *
     * <h2>⚠️ The rule keys on UNKNOWN, not on "is it text"</h2>
     *
     * <p>An earlier version refused ordering over anything not numeric or temporal, which caught
     * {@link QueryType#TEXT} as well — and that was wrong. Comparing declared text is a perfectly
     * ordinary question: <em>surnames after M</em>, <em>codes before "K-200"</em>. A data source orders
     * text by collation and means it.</p>
     *
     * <p>The danger is never text; it is <strong>text that is really a number and nobody said so</strong>.
     * That is exactly {@link QueryType#UNKNOWN} — a schema declining to promise a type. So a product that
     * declares a bag field as {@code TEXT} is making a promise, and is believed; a product that leaves it
     * {@code UNKNOWN} is saying it does not know, and then neither does the compiler.</p>
     *
     * <p>A real column also needs nothing and must not be made to ask: {@code issue.storyPoints > 5} is
     * already unambiguous, and demanding a converter there would teach people the pipe is noise to
     * sprinkle everywhere — and people who believe that stop reading refusals.</p>
     *
     * @return {@code true} when {@code &lt;} or {@code &gt;} over this attribute must be refused unless a
     *         converter was applied
     */
    public boolean needsConverterForOrdering() {
        return type == QueryType.UNKNOWN;
    }
}
