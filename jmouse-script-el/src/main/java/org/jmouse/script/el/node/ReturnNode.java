package org.jmouse.script.el.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;

/**
 * {@code return}, with or without a value.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ReturnNode extends AbstractExpression {

    private Expression value;

    /**
     * Returns what is being returned.
     *
     * @return the expression, or {@code null} for a bare {@code return}
     */
    public Expression getValue() {
        return value;
    }

    public void setValue(Expression value) {
        this.value = value;
    }

    /**
     * Leaves the enclosing body.
     *
     * <p>⚠️ It never returns normally — see {@link ReturnSignal} for why that is an exception and why
     * the exception is stackless.</p>
     *
     * @param context the evaluation context
     * @return never; the signal always leaves this method
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        throw new ReturnSignal(value == null ? null : value.evaluate(context));
    }

    @Override
    public String toSource() {
        return value == null ? "return" : "return " + value.toSource();
    }

    @Override
    public String toString() {
        return toSource();
    }

}
