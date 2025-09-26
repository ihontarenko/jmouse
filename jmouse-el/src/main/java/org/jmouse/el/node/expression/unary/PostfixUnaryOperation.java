package org.jmouse.el.node.expression.unary;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.PropertyNode;
import org.jmouse.el.node.expression.UnaryOperation;

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
    public PostfixUnaryOperation(Expression operand, Operator operator) {
        super(operand, operator);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        Object     value      = operand.evaluate(context);
        Conversion conversion = context.getConversion();
        Object     calculated = operator.getCalculator().calculate(value);

        if (operand instanceof PropertyNode property) {
            Class<?> originalType = value.getClass();
            context.setValue(property.getPath(), conversion.convert(calculated, originalType));
        }

        return value;
    }

    @Override
    public String toString() {
        return "( %s %s )".formatted(operand, operator.getName());
    }
}
