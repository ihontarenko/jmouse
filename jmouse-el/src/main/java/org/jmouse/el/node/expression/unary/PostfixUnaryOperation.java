package org.jmouse.el.node.expression.unary;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.node.ExpressionNode;
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
    public PostfixUnaryOperation(ExpressionNode operand, Operator operator) {
        super(operand, operator);
    }
}
