package org.jmouse.template.node.expression.arithmetic;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.node.ExpressionNode;

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
     * @param operand  the operand expression (e.g., a variable)
     * @param operator the prefix operator (e.g., {@code ++}, {@code --})
     */
    public PrefixUnaryOperation(ExpressionNode operand, Token.Type operator) {
        super(operand, operator);
    }
}
