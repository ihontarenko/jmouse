package org.jmouse.el.node.expression.unary;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.operator.LogicalOperator;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.UnaryOperation;

public class NegateUnaryOperation extends UnaryOperation {

    public NegateUnaryOperation(Expression operand) {
        super(operand, LogicalOperator.NOT);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return operator.getCalculator().calculate(operand.evaluate(context));
    }

    /**
     * Writes the negation back as {@code !(…)}.
     *
     * <p>⚠️ The parentheses are not optional. {@code !} binds tighter than the comparison it usually
     * wraps, so {@code !a == b} re-parses as {@code (!a) == b} — a different question with a plausible
     * answer. And {@code not} cannot be used instead: in this lexer that word is an alias for
     * {@code !=}, not a prefix negation.</p>
     */
    @Override
    public String toSource() {
        return "%s(%s)".formatted(operator.getSpelling(), operand.toSource());
    }

    @Override
    public String toString() {
        return "( %s : %s )".formatted(operator.getName(), operand);
    }

}
