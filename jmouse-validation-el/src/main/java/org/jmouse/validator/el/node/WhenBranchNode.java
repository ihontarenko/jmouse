package org.jmouse.validator.el.node;

import org.jmouse.el.node.expression.ExpressionsNode;

/**
 * One branch of a {@code when} — the guarded one, or the {@code otherwise}.
 *
 * <p>The shape is the expression language's own {@code if}: a branch carries a condition and a body,
 * and the {@code otherwise} is simply the branch whose condition is {@code null}. Modelling it that way
 * rather than as a second list is what lets {@link org.jmouse.el.language.parser.AbstractBranchParser}
 * read it, and it is the same answer {@code IfBranchNode} already gives.</p>
 *
 * <p>⚠️ The condition is text, not a compiled expression — see
 * {@link org.jmouse.validator.el.parser.ExpressionSlice} for why every expression in a {@code .jmv} is
 * kept as written and compiled by a plain {@code ExpressionLanguage} later.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class WhenBranchNode extends ExpressionsNode {

    private String condition;

    /** @return the guard, as it was written, or {@code null} for the {@code otherwise} branch */
    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    /** @return whether this branch runs only under a condition */
    public boolean isGuarded() {
        return condition != null;
    }

    @Override
    public String toString() {
        return isGuarded() ? "when " + condition : "otherwise";
    }
}
