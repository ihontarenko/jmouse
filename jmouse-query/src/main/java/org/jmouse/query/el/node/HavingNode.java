package org.jmouse.query.el.node;

import org.jmouse.el.node.Expression;
import org.jmouse.query.translate.Capability;

/**
 * {@code having <expression>} — which groups survive.
 *
 * <h2>⚠️ Not a second {@code where}, and the difference is the classic confusion</h2>
 *
 * <p>{@code where} filters <strong>rows before they are gathered</strong>; this filters
 * <strong>groups afterwards</strong>. An aggregate is meaningless in the first and is the entire point
 * of the second.</p>
 *
 * <p>So the two are separate clauses with separate names rather than one clause that behaves differently
 * depending on what is written in it. A person who puts {@code count() > 3} in a {@code where} gets a
 * refusal that explains the difference — not a database error about an invalid group function, which is
 * what conflating them would have produced.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class HavingNode extends ClauseNode {

    public static final ClauseKind KIND =
            ClauseKind.of("having", Capability.AGGREGATE, 4 * ClauseKind.STEP);

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

    @Override
    protected String bodyToSource() {
        return condition == null ? "" : condition.toSource();
    }
}
