package org.jmouse.jdbc.dialect;

/**
 * MySQL — backticks, {@code SIGNED}, and a case-insensitive collation by default.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class MySqlDialect implements Dialect {

    /** What a value has to look like before it is read as a whole number. */
    private static final String INTEGER_SHAPE = "'^[+-]?[0-9]+$'";

    /** The same, allowing a decimal point and an exponent. */
    private static final String DECIMAL_SHAPE = "'^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)([eE][+-]?[0-9]+)?$'";

    @Override
    public String name() {
        return "mysql";
    }

    /**
     * ⚠️ A backtick inside a name is doubled, not escaped with a backslash — MySQL's own rule, and the
     * one thing that keeps this from being a way to end the identifier early.
     */
    @Override
    public String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * ⚠️ Guarded, because a bare {@code CAST('n/a' AS SIGNED)} yields <strong>0</strong> here — with a
     * warning nobody reads — so {@code < 5} would be true for a row holding "n/a". PostgreSQL raises on
     * the same value, so without the guard the two databases answer the identical query differently.
     */
    @Override
    public String textAsIntegerTemplate() {
        return "CASE WHEN %s REGEXP " + INTEGER_SHAPE + " THEN CAST(%s AS SIGNED) END";
    }

    @Override
    public String textAsDecimalTemplate() {
        return "CASE WHEN %s REGEXP " + DECIMAL_SHAPE + " THEN CAST(%s AS DECIMAL(38, 10)) END";
    }

    /**
     * ⚠️ {@code LOWER} on both sides rather than trusting the column's collation. MySQL's default
     * collations usually compare case-insensitively and a binary one does not — so a query written
     * against one table would behave differently against another in the same database, silently.
     */
    @Override
    public String caseInsensitiveLike(String expression, String pattern) {
        return "LOWER(%s) LIKE LOWER(%s)".formatted(expression, pattern);
    }

    @Override
    public String before(String expression, String separator) {
        return "SUBSTRING_INDEX(%s, %s, 1)".formatted(expression, separator);
    }

    @Override
    public String after(String expression, String separator) {
        return "SUBSTRING_INDEX(%s, %s, -1)".formatted(expression, separator);
    }

    @Override
    public String concatenate(String left, String right) {
        return "CONCAT(%s, %s)".formatted(left, right);
    }

    @Override
    public String shift(String moment, String amount, String unit, boolean subtract) {
        return "%s(%s, INTERVAL %s %s)".formatted(subtract ? "DATE_SUB" : "DATE_ADD", moment, amount, unit);
    }

    @Override
    public String limit(long limit, long offset) {
        return offset > 0 ? "LIMIT %d OFFSET %d".formatted(limit, offset) : "LIMIT %d".formatted(limit);
    }
}
