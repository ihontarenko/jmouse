package org.jmouse.query.el.node;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.query.translate.Capability;
import org.jmouse.query.el.dialect.QueryLogicalOperator;

/**
 * {@code where} — which rows the query is about.
 *
 * <h2>⚠️ It may be written more than once, and repeats are an {@code and}</h2>
 *
 * <p>That is not a convenience. A product composing onto a filter somebody else wrote — a tenant
 * condition, a scope, a board's own rule — would otherwise have to take that person's expression apart
 * and put it back together, which means implementing the language a second time in order to add one
 * comparison to it. Two clauses, combined here, cost nobody a parser.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class WhereNode extends ClauseNode {

    public static final ClauseKind KIND =
            ClauseKind.of("where", Capability.FILTER, 2 * ClauseKind.STEP).repeating();

    public static final String KEYWORD = KIND.keyword();

    private Expression condition;

    public Expression getCondition() {
        return condition;
    }

    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    @Override
    public ClauseKind kind() {
        return KIND;
    }

    /**
     * ⚠️ Combines by building an {@code and} rather than by concatenating text, so precedence is the
     * tree's business and not a bracketing rule somebody has to remember.
     */
    @Override
    public void merge(ClauseNode other) {
        Expression second = ((WhereNode) other).getCondition();

        if (second == null) {
            return;
        }

        condition = condition == null
                ? second
                : new BinaryOperation(condition, QueryLogicalOperator.AND, second);
    }

    @Override
    protected String bodyToSource() {
        return condition == null ? "" : condition.toSource();
    }
}
