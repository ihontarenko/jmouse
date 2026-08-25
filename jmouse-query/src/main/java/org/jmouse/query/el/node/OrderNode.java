package org.jmouse.query.el.node;

import org.jmouse.el.node.Expression;
import org.jmouse.query.translate.Capability;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code order <expression> [asc|desc] {, …}} — in what sequence.
 *
 * <p>⚠️ <strong>A key is an expression, not a column name</strong>, and that is most of why a document
 * was worth having at all: {@code order entry[quantity] | int desc} needs the converter as much as a
 * {@code where} does. Sorting a bag of text without one sorts {@code "900"} after {@code "1000"}, and
 * an envelope of three string fields would have had to invent a second syntax to say so.</p>
 *
 * <p>Several keys, in the order written — a single-key clause was the shape this started as, and the
 * first real dashboard asked for a tie-breaker.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class OrderNode extends ClauseNode {

    public static final ClauseKind KIND =
            ClauseKind.of("order", Capability.SORT, 5 * ClauseKind.STEP);

    public static final String KEYWORD = KIND.keyword();

    private final List<Key> keys = new ArrayList<>();

    public void addKey(Expression expression, Direction direction) {
        keys.add(new Key(expression, direction));
    }

    public List<Key> getKeys() {
        return List.copyOf(keys);
    }

    @Override
    public ClauseKind kind() {
        return KIND;
    }

    @Override
    protected String bodyToSource() {
        return keys.stream().map(Key::toSource).collect(Collectors.joining(", "));
    }

    /** Which way a key runs. */
    public enum Direction {
        ASCENDING("asc"),
        DESCENDING("desc");

        private final String spelling;

        Direction(String spelling) {
            this.spelling = spelling;
        }

        public String spelling() {
            return spelling;
        }
    }

    /**
     * One sort key.
     *
     * <p>⚠️ The direction is kept even when it is the default. A person who typed {@code asc} sees
     * {@code asc} when the document is written back — the round trip preserves what was written, not
     * what the writer would have chosen.</p>
     *
     * @param expression what to sort by
     * @param direction  which way, or {@code null} when it was not written
     */
    public record Key(Expression expression, Direction direction) {

        public String toSource() {
            String rendered = expression.toSource();

            return direction == null ? rendered : "%s %s".formatted(rendered, direction.spelling());
        }
    }
}
