package org.jmouse.template.node.expression.arithmetic;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.node.AbstractExpressionNode;
import org.jmouse.template.node.ExpressionNode;

/**
 * Represents a unary operation expression node in the Abstract Syntax Tree (AST).
 *
 * <p>This node is used to represent unary operations such as increment, decrement,
 * unary plus, or unary minus. It extends {@link AbstractExpressionNode} and holds a single operand
 * along with an operator represented by a {@link Token.Type}.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
abstract public class UnaryOperation extends AbstractExpressionNode {

    /**
     * The operator for the unary operation.
     *
     * <p>This field can represent operations such as increment, decrement, unary plus, or unary minus,
     * and is represented using a {@link Token.Type} value.</p>
     */
    private final Token.Type operator;

    /**
     * The operand on which the unary operation is applied.
     */
    private final ExpressionNode operand;

    /**
     * Constructs a {@code UnaryOperation} node with the specified operand and operator.
     *
     * @param operand  the operand expression on which the unary operation is applied
     * @param operator the token type representing the unary operator (e.g., increment, decrement, plus, minus)
     */
    public UnaryOperation(ExpressionNode operand, Token.Type operator) {
        this.operator = operator;
        this.operand = operand;
    }

    /**
     * Returns the operator for this unary operation.
     *
     * @return the {@link Token.Type} representing the operator
     */
    public Token.Type getOperator() {
        return operator;
    }

    /**
     * Returns the operand expression of this unary operation.
     *
     * @return the operand expression
     */
    public ExpressionNode getOperand() {
        return operand;
    }
}
