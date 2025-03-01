package org.jmouse.template.node;

import org.jmouse.template.lexer.Token;

/**
 * Represents a unary operation expression node in the AST.
 * <p>
 * This node is used to represent unary operations such as increment, decrement,
 * unary plus, or unary minus. It extends {@link AbstractExpression} and holds a single operand
 * along with an operator represented by a {@link Token.Type}.
 * </p>
 *
 * @author ...
 * @version 1.0
 */
public class UnaryOperation extends AbstractExpression {

    /**
     * The operator for the unary operation.
     * <p>
     * This field can represent operations such as increment, decrement, unary plus, or unary minus,
     * and is represented using a {@link Token.Type} value.
     * </p>
     */
    private final Token.Type operator;

    /**
     * Constructs a UnaryOperation node with the specified operand and operator.
     *
     * @param operand  the operand expression on which the unary operation is applied
     * @param operator the token type representing the unary operator (e.g., increment, decrement, plus, minus)
     */
    public UnaryOperation(Expression operand, Token.Type operator) {
        super(operand);
        this.operator = operator;
    }

    /**
     * Returns the operator for this unary operation.
     *
     * @return the {@link Token.Type} representing the operator
     */
    public Token.Type getOperator() {
        return operator;
    }
}
