package org.jmouse.jdbc.dialect;

/**
 * PostgreSQL — double quotes, {@code ILIKE}, {@code ||}, and a cast that raises rather than guessing.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PostgreSqlDialect implements Dialect {

    /** What a value has to look like before it is read as a whole number. */
    private static final String INTEGER_SHAPE = "'^[+-]?[0-9]+$'";

    /** The same, allowing a decimal point and an exponent. */
    private static final String DECIMAL_SHAPE = "'^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)([eE][+-]?[0-9]+)?$'";

    @Override
    public String name() {
        return "postgresql";
    }

    /**
     * ⚠️ A double quote inside a name is doubled. And ⚠️ quoting is not free here in a second way that
     * MySQL does not share: PostgreSQL folds an unquoted name to <em>lower case</em>, so quoting a name
     * that was created unquoted in mixed case will not find it. Callers pass the name as it exists.
     */
    @Override
    public String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /**
     * ⚠️ Guarded, because a bare {@code CAST('n/a' AS INTEGER)} <strong>raises</strong> here — so a single
     * unparseable row anywhere kills the query for every row that was fine. MySQL instead yields 0, so
     * without the guard the two databases answer the identical query differently.
     */
    @Override
    public String textAsIntegerTemplate() {
        return "CASE WHEN %s ~ " + INTEGER_SHAPE + " THEN CAST(%s AS BIGINT) END";
    }

    @Override
    public String textAsDecimalTemplate() {
        return "CASE WHEN %s ~ " + DECIMAL_SHAPE + " THEN CAST(%s AS NUMERIC(38, 10)) END";
    }

    /**
     * ⚠️ {@code LOWER(…) LIKE LOWER(…)} rather than {@code ILIKE}, deliberately. {@code ILIKE} is
     * PostgreSQL's own and reads better, but it is not what MySQL does, and the point of this pair is
     * that the same query means the same thing on both. The one place a difference is worth having is
     * where it is unavoidable.
     */
    @Override
    public String caseInsensitiveLike(String expression, String pattern) {
        return "LOWER(%s) LIKE LOWER(%s)".formatted(expression, pattern);
    }

    @Override
    public String before(String expression, String separator) {
        return "split_part(%s, %s, 1)".formatted(expression, separator);
    }

    /**
     * ⚠️ A negative index needs PostgreSQL 14 or later. Earlier versions return an empty string instead
     * of the trailing part — a <em>silent</em> disagreement with MySQL rather than an error, which is
     * worth knowing before deploying against an older server.
     */
    @Override
    public String after(String expression, String separator) {
        return "split_part(%s, %s, -1)".formatted(expression, separator);
    }

    @Override
    public String concatenate(String left, String right) {
        return "(%s || %s)".formatted(left, right);
    }

    @Override
    public String shift(String moment, String amount, String unit, boolean subtract) {
        return "(%s %s %s * INTERVAL '1 %s')".formatted(moment, subtract ? "-" : "+", amount, unit);
    }

    @Override
    public String limit(long limit, long offset) {
        return offset > 0 ? "LIMIT %d OFFSET %d".formatted(limit, offset) : "LIMIT %d".formatted(limit);
    }
}
