package org.jmouse.el.node.expression.unary;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.operator.SignOperator;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.UnaryOperation;

/** Arithmetic negation, {@code -x}: the number with its sign changed and its type kept. */
public class NegativeUnaryOperation extends UnaryOperation {

    public NegativeUnaryOperation(Expression operand) {
        super(operand, SignOperator.NEGATIVE);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return operator.getCalculator().calculate(operand.evaluate(context));
    }

    /**
     * Writes the negation back as {@code -(…)}: without the parentheses {@code -(a - b)} would come back as
     * {@code -a - b}, and {@code -(-1)} as {@code --1}, which a language with step operators reads as a decrement.
     */
    @Override
    public String toSource() {
        return "-(" + operand.toSource() + ")";
    }

    @Override
    public String toString() {
        return "( %s : %s )".formatted(operator.getName(), operand);
    }
}
