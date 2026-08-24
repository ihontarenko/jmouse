package org.jmouse.query.schema;

import org.jmouse.query.el.QueryParseException;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * A query that parsed but does not make sense against the data it names.
 *
 * <p>Separate from a parse failure on purpose: *"this is not valid jMQ"* and *"there is nothing here
 * called that"* send a reader to two entirely different places, and a message that conflates them sends
 * them to neither.</p>
 *
 * <p>⚠️ <strong>Every message names the fix.</strong> These are the sentences somebody meets while
 * composing a filter in a text box, and they are the only teaching this language does.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryCheckException extends QueryParseException {

    /** How many suggestions a refusal offers before the list stops being help and becomes noise. */
    private static final int SUGGESTION_LIMIT = 5;

    public QueryCheckException(String message) {
        super(message);
    }

    /**
     * Nothing here is called that.
     *
     * <p>Offers what is, because the overwhelmingly common cause is a name remembered slightly wrong,
     * and a reader who can see the real list fixes it without leaving the box.</p>
     *
     * @param name       what was written
     * @param attributes what could have been written
     * @return the refusal to throw
     */
    public static QueryCheckException unknownAttribute(String name, Collection<QueryAttribute> attributes) {
        String offered = attributes.stream()
                .map(QueryAttribute::name)
                .sorted(Comparator.naturalOrder())
                .limit(SUGGESTION_LIMIT)
                .collect(Collectors.joining(", "));

        if (offered.isEmpty()) {
            return new QueryCheckException(
                    "there is nothing called '%s' here, and nothing else either — this target has no described attributes"
                            .formatted(name));
        }

        String more = attributes.size() > SUGGESTION_LIMIT
                ? ", and %d more".formatted(attributes.size() - SUGGESTION_LIMIT)
                : "";

        return new QueryCheckException(
                "there is nothing called '%s' here; this target has %s%s".formatted(name, offered, more));
    }

    /**
     * An ordered comparison over a value whose type nobody promised.
     *
     * <p>⚠️ <strong>The refusal this schema exists for.</strong> Compared as text, {@code "900" > "1000"}
     * is <em>true</em> — because {@code "9" > "1"} — so the query would answer wrongly on every row and
     * say nothing about it. The message spells the fix out, because four characters is the whole
     * difference between a working filter and a silent lie.</p>
     *
     * @param path     the attribute as it was written
     * @param operator the comparison that needs a type
     * @return the refusal to throw
     */
    public static QueryCheckException untypedComparison(String path, String operator) {
        return new QueryCheckException(
                ("nobody has said what kind of value '%s' holds, so comparing it with %s would compare "
                 + "words rather than values — and as words \"900\" is greater than \"1000\". "
                 + "Say how to read it: '%s | int', or '| double', or '| bigDecimal'")
                        .formatted(path, operator, path));
    }

    /**
     * Sorting by a value whose type nobody promised.
     *
     * <p>⚠️ <strong>Its own message, because the comparison one reads as nonsense here</strong> —
     * "comparing it with sorting" is not a sentence. And this is the half people forget: a list sorted by
     * untyped text puts 900 after 1000 exactly as surely as a filter does, except that a sorted screen
     * carries no hint at all that it is wrong.</p>
     *
     * @param path the attribute as it was written
     * @return the refusal to throw
     */
    public static QueryCheckException untypedOrdering(String path) {
        return new QueryCheckException(
                ("nobody has said what kind of value '%s' holds, so sorting by it would sort words rather "
                 + "than values — 900 would come after 1000. "
                 + "Say how to read it: '%s | int', or '| double', or '| bigDecimal'")
                        .formatted(path, path));
    }
}
