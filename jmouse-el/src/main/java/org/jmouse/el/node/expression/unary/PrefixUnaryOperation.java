package org.jmouse.el.node.expression.unary;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.PropertyNode;
import org.jmouse.el.node.expression.UnaryOperation;

import static org.jmouse.el.extension.operator.UnaryOperator.DECREMENT;
import static org.jmouse.el.extension.operator.UnaryOperator.INCREMENT;

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
    public PrefixUnaryOperation(Expression operand, Operator operator) {
        super(operand, operator);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        Object     value      = operand.evaluate(context);
        Conversion conversion = context.getConversion();

        if (operand instanceof PropertyNode property && (operator == INCREMENT || operator == DECREMENT)) {
            Class<?> originalType = value.getClass();

            value = operator.getCalculator().calculate(value);
            value = conversion.convert(value, originalType);

            context.setValue(property.getPath(), value);
        }

        return value;
    }

    /**
     * Writes the operation back — {@code !x}, {@code ++i}.
     *
     * <p>No space between operator and operand: {@code ++ i} would lex as two separate {@code +}
     * operators followed by a name, which parses and means something else entirely.</p>
     */
    @Override
    public String toSource() {
        return "%s%s".formatted(operator.getSpelling(), operand.toSource());
    }

    @Override
    public String toString() {
        return "( %s %s )".formatted(operator.getName(), operand);
    }
}
