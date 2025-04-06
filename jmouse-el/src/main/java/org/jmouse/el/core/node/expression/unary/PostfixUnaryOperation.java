package org.jmouse.el.core.node.expression.unary;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.extension.Operator;
import org.jmouse.el.core.node.ExpressionNode;
import org.jmouse.el.core.node.expression.PropertyNode;
import org.jmouse.el.core.node.expression.UnaryOperation;

import static org.jmouse.el.core.extension.operator.UnaryOperator.DECREMENT;
import static org.jmouse.el.core.extension.operator.UnaryOperator.INCREMENT;

/**
 * Represents a postfix unary operation in the Abstract Syntax Tree (AST).
 *
 * <p>This node is used for operations where the operator appears after the operand,
 * such as post-increment ({@code i++}) or post-decrement ({@code i--}).</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PostfixUnaryOperation extends UnaryOperation {

    /**
     * Constructs a {@code PostfixUnaryOperation} with the specified operand and operator.
     *
     * @param operand  the operand tag (e.g., a variable)
     * @param operator the postfix operator (e.g., {@code ++}, {@code --})
     */
    public PostfixUnaryOperation(ExpressionNode operand, Operator operator) {
        super(operand, operator);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        Object value = operand.evaluate(context);

        if (operand instanceof PropertyNode property && (operator == INCREMENT || operator == DECREMENT)) {
            Class<?> originalType = value.getClass();
            context.setValue(property.getPath(), context.getConversion().convert(
                    operator.getCalculator().calculate(value), originalType));
        }

        return value;
    }

    @Override
    public String toString() {
        return "( %s %s )".formatted(operand, operator.getName());
    }
}
