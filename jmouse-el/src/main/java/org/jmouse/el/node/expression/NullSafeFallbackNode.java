package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.ExpressionNode;

/**
 * Represents a null-coalescing expression in the view language.
 * <p>
 * Evaluates the {@code nullable} expression first; if its result is non-null,
 * that value is returned. Otherwise, the {@code otherwise} expression is evaluated
 * and its result returned.
 * </p>
 * <p>
 * Syntax example in a view:
 * <pre>
 *   {{ user.name ?? "Anonymous" }}
 * </pre>
 * </p>
 */
public class NullSafeFallbackNode extends AbstractExpressionNode {

    /**
     * The expression whose result may be null.
     */
    private ExpressionNode nullable;

    /**
     * The expression to evaluate if {@code nullable} returns null.
     */
    private ExpressionNode otherwise;

    /**
     * Returns the nullable expression.
     *
     * @return the expression that may yield null
     */
    public ExpressionNode getNullable() {
        return nullable;
    }

    /**
     * Sets the nullable expression.
     *
     * @param nullable the expression to evaluate first
     */
    public void setNullable(ExpressionNode nullable) {
        this.nullable = nullable;
    }

    /**
     * Returns the fallback expression.
     *
     * @return the expression evaluated if {@code nullable} is null
     */
    public ExpressionNode getOtherwise() {
        return otherwise;
    }

    /**
     * Sets the fallback expression.
     *
     * @param otherwise the expression to evaluate when {@code nullable} is null
     */
    public void setOtherwise(ExpressionNode otherwise) {
        this.otherwise = otherwise;
    }

    /**
     * Evaluates the null-coalescing expression.
     * <p>
     * First evaluates {@code nullable}. If the result is non-null, it is returned;
     * otherwise {@code otherwise} is evaluated and its result returned.
     * </p>
     *
     * @param context the evaluation context providing variable resolution and conversions
     * @return the first non-null result of the two expressions
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        Object result = nullable.evaluate(context);

        if (result == null) {
            result = otherwise.evaluate(context);
        }

        return result;
    }

    /**
     * Returns a string representation of the null-coalescing expression.
     * <p>
     * Format: {@code "<nullable> ?? <otherwise>"}.
     * </p>
     *
     * @return a string representation of this node
     */
    @Override
    public String toString() {
        return "%s ?? %s".formatted(nullable, otherwise);
    }
}
