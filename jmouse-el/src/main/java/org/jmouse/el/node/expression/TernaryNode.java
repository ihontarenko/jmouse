package org.jmouse.el.node.expression;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;

/**
 * Represents a ternary (conditional) expression in the view language.
 * <p>
 * The expression evaluates the {@code condition}. If it converts to {@code true},
 * the {@code thenBranch} is evaluated and returned; otherwise the {@code elseBranch}
 * is evaluated and returned.
 * </p>
 * <p>
 * Syntax example in a view:
 * <pre>
 *   {{ user.isAdmin ? "Admin" : "User" }}
 * </pre>
 * </p>
 */
public class TernaryNode extends AbstractExpression {

    /**
     * The condition expression to evaluate first.
     */
    private Expression condition;
    /**
     * The expression to evaluate if the condition is true.
     */
    private Expression thenBranch;
    /**
     * The expression to evaluate if the condition is false.
     */
    private Expression elseBranch;

    /**
     * Returns the condition expression.
     *
     * @return the condition {@link Expression}
     */
    public Expression getCondition() {
        return condition;
    }

    /**
     * Sets the condition expression.
     *
     * @param condition the condition {@link Expression} to set
     */
    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    /**
     * Returns the 'then' branch expression.
     *
     * @return the {@link Expression} for the true case
     */
    public Expression getThenBranch() {
        return thenBranch;
    }

    /**
     * Sets the 'then' branch expression.
     *
     * @param thenBranch the {@link Expression} to evaluate if the condition is true
     */
    public void setThenBranch(Expression thenBranch) {
        this.thenBranch = thenBranch;
    }

    /**
     * Returns the 'else' branch expression.
     *
     * @return the {@link Expression} for the false case
     */
    public Expression getElseBranch() {
        return elseBranch;
    }

    /**
     * Sets the 'else' branch expression.
     *
     * @param elseBranch the {@link Expression} to evaluate if the condition is false
     */
    public void setElseBranch(Expression elseBranch) {
        this.elseBranch = elseBranch;
    }

    /**
     * Evaluates the ternary expression.
     * <p>
     * First evaluates the {@code condition} expression. Converts the result to {@link Boolean}.
     * If {@code true}, evaluates and returns {@code thenBranch}; otherwise evaluates and returns {@code elseBranch}.
     * </p>
     *
     * @param context the evaluation context providing conversion and variable resolution
     * @return the result of either the then- or else- branch evaluation
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        Conversion conversion = context.getConversion();
        Object     satisfied  = condition.evaluate(context);

        return conversion.convert(satisfied, Boolean.class)
                ? thenBranch.evaluate(context) : elseBranch.evaluate(context);
    }

    /**
     * Returns a string representation of this ternary expression.
     *
     * @return a formatted string "(condition ? thenBranch : elseBranch)"
     */
    @Override
    public String toString() {
        return "TERNARY(%s ? %s : %s)".formatted(condition, thenBranch, elseBranch);
    }
}
