package org.jmouse.formula.el;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.UnaryOperation;

/** Arithmetic negation, -x: the one prefix jME's own nodes do not have (its unary node is logical NOT). */
public class NegationNode extends UnaryOperation {

    public NegationNode(Expression operand) {
        super(operand, FormulaOperator.NEGATE);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return operator.getCalculator().calculate(operand.evaluate(context));
    }

    @Override
    public String toSource() {
        return "-(" + operand.toSource() + ")";
    }

    @Override
    public String toString() {
        return "( NEGATE : " + operand + " )";
    }
}
