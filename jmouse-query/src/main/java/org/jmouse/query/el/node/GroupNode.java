package org.jmouse.query.el.node;

import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code group <expression> {, …}} — what the rows are gathered by.
 *
 * <p>⚠️ <strong>A group clause changes what a result IS.</strong> Without one a row is a row of the
 * underlying thing; with one it is a tuple, and everything reading the result — a screen, an export, an
 * agent — has to be told which it is getting. That is why {@code CompiledQuery} reports it rather than
 * leaving a caller to notice.</p>
 *
 * <p>⚠️ And paging over a grouped query counts <em>groups</em>, not rows. Getting that wrong produces a
 * count that looks entirely plausible and is wrong.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class GroupNode extends ClauseNode {

    public static final String KEYWORD = "group";

    private final List<Expression> keys = new ArrayList<>();

    public void addKey(Expression key) {
        keys.add(key);
    }

    public List<Expression> getKeys() {
        return List.copyOf(keys);
    }

    @Override
    public String keyword() {
        return KEYWORD;
    }

    @Override
    protected String bodyToSource() {
        return keys.stream().map(Expression::toSource).collect(Collectors.joining(", "));
    }
}
