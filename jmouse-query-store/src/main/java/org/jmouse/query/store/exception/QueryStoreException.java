package org.jmouse.query.store.exception;

/**
 * 🚫 Something went wrong keeping a query, as opposed to reading or running one.
 *
 * <p>Kept apart from a parse failure and from a schema refusal on purpose: those two are things a
 * person can fix in the text box in front of them, and this one is not.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryStoreException extends RuntimeException {

    public QueryStoreException(String message) {
        super(message);
    }

    public QueryStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Nothing describes the source a query says it queries.
     *
     * <p>⚠️ The refusal that stops a saved query becoming a mystery later. A row naming a source that
     * no product describes cannot be run, cannot be checked, and cannot be repaired by whoever finds it
     * — so it is refused at the moment somebody could still say what they meant.</p>
     *
     * @param source what the query named
     * @return the refusal to throw
     */
    public static QueryStoreException unknownSource(String source) {
        return new QueryStoreException(
                "'%s' is not a source anything here describes, so a query written against it could never be run"
                        .formatted(source));
    }

    /**
     * A field longer than its column.
     *
     * <p>⚠️ Refused here rather than at the database, which reports it as a truncation naming a column
     * and a row — a sentence that reaches a log rather than the person who was typing.</p>
     *
     * @param field   what was too long, in a person's words
     * @param length  how long it was
     * @param maximum how long it may be
     * @return the refusal to throw
     */
    public static QueryStoreException tooLong(String field, int length, int maximum) {
        return new QueryStoreException(
                "the %s is %d characters, and may be at most %d".formatted(field, length, maximum));
    }

    /**
     * A query with nothing where something is required.
     *
     * @param field what is missing, in a person's words
     * @return the refusal to throw
     */
    public static QueryStoreException missing(String field) {
        return new QueryStoreException("a saved query needs %s".formatted(field));
    }
}
