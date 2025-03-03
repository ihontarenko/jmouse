package org.jmouse.template.node.arithmetic;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.node.Expression;

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
     * @param operand  the operand expression (e.g., a variable)
     * @param operator the postfix operator (e.g., {@code ++}, {@code --})
     */
    public PostfixUnaryOperation(Expression operand, Token.Type operator) {
        super(operand, operator);
    }
}
