package org.jmouse.jdbc.dialect;

/**
 * The handful of places two databases disagree about how to write the same idea.
 *
 * <p>Deliberately small. This is not an attempt to abstract SQL — it is a list of the differences
 * something generating SQL actually trips over, and it grows one method at a time, when a generator
 * needs one. An interface full of methods nobody calls is a maintenance surface that never earns its
 * keep.</p>
 *
 * <p>⚠️ <strong>It lives here rather than beside a query language on purpose.</strong> Knowing how MySQL
 * quotes an identifier is useful to a migration helper, a paging helper and a bulk writer as much as to
 * a compiler; putting it in a query module would make every one of them depend on a query language to
 * find out.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Dialect {

    /** What this dialect is called, for a message that has to name it. */
    String name();

    /**
     * Wraps a table or column name so that a reserved word or an unusual character is read as a name.
     *
     * @param identifier the bare name
     * @return the name, quoted this dialect's way
     */
    String quote(String identifier);

    /**
     * Reads a text value as a whole number, or as {@code NULL} when it is not one.
     *
     * <h2>⚠️ Why this is not simply {@code CAST(x AS …)}</h2>
     *
     * <p>The two databases behave <strong>differently</strong> on a value that is not a number, and both
     * behaviours are wrong for a query:</p>
     *
     * <ul>
     *   <li>MySQL yields <strong>0</strong> and a warning nobody reads. So {@code CAST('n/a' AS SIGNED) < 5}
     *       is <em>true</em>, and a row holding "n/a" quietly answers a question about quantities.</li>
     *   <li>PostgreSQL <strong>raises</strong>, so one unparseable row anywhere in the table kills the
     *       whole query — including for every row that was perfectly fine.</li>
     * </ul>
     *
     * <p>Neither is acceptable, and — worse — they are not the same, so the identical query would return
     * different answers on the two. A value stored as text in a bag holds whatever somebody typed, which
     * makes this ordinary traffic rather than an edge case.</p>
     *
     * <p>⚠️ <strong>So both dialects guard the cast and yield {@code NULL} instead.</strong> A value that
     * is not a number satisfies no numeric comparison, which is exactly what SQL's three-valued logic
     * already does with an unknown — and it is the same answer on both databases.</p>
     *
     * <h2>⚠️ A TEMPLATE, because the value has to be named twice</h2>
     *
     * <p>A guarded cast tests the value and then casts it, so {@code %s} appears <strong>more than
     * once</strong>. That is why this returns a template rather than finished SQL: a caller substituting
     * an expression that itself binds parameters has to repeat those parameters once per occurrence, and
     * it can only know to do that if the repetition is visible.</p>
     *
     * <p>⚠️ Against a bare column the difference is invisible — a column reference binds nothing. It
     * appears the first time somebody writes {@code entry[x] | before("|") | int}, and a caller that
     * substituted the text twice while binding once produces a statement whose placeholders and values
     * no longer line up.</p>
     *
     * @return SQL with {@code %s} everywhere the value goes, yielding a whole number or {@code NULL}
     */
    String textAsIntegerTemplate();

    /**
     * Reads a text value as a fractional number, or as {@code NULL} when it is not one.
     *
     * @return SQL with {@code %s} everywhere the value goes, yielding a decimal or {@code NULL}
     * @see #textAsIntegerTemplate()
     */
    String textAsDecimalTemplate();

    /**
     * Compares text ignoring case — what {@code contains}, {@code starts} and {@code ends} become.
     *
     * @param expression the SQL expression holding text
     * @param pattern    the SQL expression holding the pattern, usually a bound parameter
     * @return a boolean expression
     */
    String caseInsensitiveLike(String expression, String pattern);

    /**
     * Everything up to the first occurrence of a separator — {@code "3300|mΩ"} becomes {@code 3300}.
     *
     * <h2>⚠️ Why the separator is LITERAL and this is not called {@code split}</h2>
     *
     * <p>An expression language's {@code split} takes a <strong>regular expression</strong>; both
     * databases take a <strong>literal</strong> delimiter. Translating one into the other disagrees in
     * opposite directions and neither complains — {@code split("|")} splits on every character in
     * memory and on the pipe in SQL, while {@code split("\\|")} does the reverse. A filter with a
     * literal contract is the only shape both worlds can honour identically.</p>
     *
     * <p>⚠️ <strong>Only the first part and the last part, deliberately — no arbitrary nth.</strong> Both
     * databases express those with one use of the separator; a general nth needs the separator twice on
     * MySQL and once on PostgreSQL, which means the two would bind a different number of parameters for
     * one expression. Two clear names cost less than an index whose translation differs by dialect.</p>
     *
     * <p>A value with no separator comes back whole — the answer both databases give.</p>
     *
     * @param expression the SQL expression holding text
     * @param separator  the SQL expression holding the separator, usually a bound parameter
     * @return an expression yielding the leading part
     */
    String before(String expression, String separator);

    /**
     * Everything after the <em>last</em> occurrence of a separator — {@code "3300|mΩ"} becomes
     * {@code mΩ}.
     *
     * @param expression the SQL expression holding text
     * @param separator  the SQL expression holding the separator, usually a bound parameter
     * @return an expression yielding the trailing part
     * @see #before(String, String)
     */
    String after(String expression, String separator);

    /**
     * Joins two text expressions.
     *
     * @param left  the first expression
     * @param right the second
     * @return an expression yielding the two run together
     */
    String concatenate(String left, String right);

    /**
     * Adds or subtracts a length of time from a moment.
     *
     * <p>⚠️ Written by the dialect because the two databases disagree in shape rather than in spelling —
     * {@code DATE_ADD(x, INTERVAL n DAY)} against {@code x + n * interval '1 day'}. A caller assembling
     * either by hand would be assembling the other one wrongly.</p>
     *
     * <p>⚠️ The amount arrives as SQL, normally a bound parameter, and the unit as one of
     * {@code SECOND MINUTE HOUR DAY WEEK MONTH YEAR}. The unit is NOT a parameter in either database —
     * it is syntax — so it is checked against that list before it is written.</p>
     *
     * @param moment   the SQL expression holding a moment
     * @param amount   the SQL expression holding how many
     * @param unit     which unit, upper case
     * @param subtract whether to go backwards
     * @return an expression yielding the shifted moment
     */
    String shift(String moment, String amount, String unit, boolean subtract);

    /**
     * Bounds a result set.
     *
     * @param limit  how many rows at most
     * @param offset how many to skip, or {@code 0}
     * @return the clause, without a leading space
     */
    String limit(long limit, long offset);
}
