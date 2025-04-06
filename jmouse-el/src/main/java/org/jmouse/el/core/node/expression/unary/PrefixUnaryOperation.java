package org.jmouse.el.core.node.expression.unary;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.extension.Operator;
import org.jmouse.el.core.node.ExpressionNode;
import org.jmouse.el.core.node.expression.PropertyNode;
import org.jmouse.el.core.node.expression.UnaryOperation;

import static org.jmouse.el.core.extension.operator.UnaryOperator.DECREMENT;
import static org.jmouse.el.core.extension.operator.UnaryOperator.INCREMENT;

/**
 * Represents a prefix unary operation in the Abstract Syntax Tree (AST).
 *
 * <p>This node is used for operations where the operator appears before the operand,
 * such as pre-increment ({@code ++i}) or pre-decrement ({@code --i}).</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PrefixUnaryOperation extends UnaryOperation {

    /**
     * Constructs a {@code PrefixUnaryOperation} with the specified operand and operator.
     *
     * @param operand  the operand tag (e.g., a variable)
     * @param operator the prefix operator (e.g., {@code ++}, {@code --})
     */
    public PrefixUnaryOperation(ExpressionNode operand, Operator operator) {
        super(operand, operator);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        Object value = operator.getCalculator().calculate(operand.evaluate(context));

        if (operand instanceof PropertyNode property && (operator == INCREMENT || operator == DECREMENT)) {
            context.setValue(property.getPath(), value);
        }

        return value;
    }

    @Override
    public String toString() {
        return "( %s %s )".formatted(operator.getName(), operand);
    }
}
