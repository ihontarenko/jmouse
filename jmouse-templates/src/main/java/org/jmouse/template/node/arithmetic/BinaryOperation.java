package org.jmouse.template.node.arithmetic;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.node.AbstractExpression;
import org.jmouse.template.node.Expression;

/**
 * Represents a binary operation node in the Abstract Syntax Tree (AST).
 *
 * <p>A binary operation consists of two expressions (left and right) and an operator,
 * such as addition, subtraction, multiplication, or division.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class BinaryOperation extends AbstractExpression {

    /**
     * The left-hand side expression of the binary operation.
     */
    private final Expression left;

    /**
     * The operator used in the binary operation.
     */
    private final Token.Type operator;

    /**
     * The right-hand side expression of the binary operation.
     */
    private final Expression right;

    /**
     * Constructs a {@code BinaryOperation} with the specified left-hand side expression,
     * operator, and right-hand side expression.
     *
     * @param left     the left-hand side expression of the binary operation
     * @param operator the operator used in the binary operation
     * @param right    the right-hand side expression of the binary operation
     */
    public BinaryOperation(Expression left, Token.Type operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    /**
     * Returns the left-hand side expression of the binary operation.
     *
     * @return the left-hand side expression
     */
    public Expression getLeft() {
        return left;
    }

    /**
     * Returns the right-hand side expression of the binary operation.
     *
     * @return the right-hand side expression
     */
    public Expression getRight() {
        return right;
    }

    /**
     * Returns the operator used in the binary operation.
     *
     * @return the operator
     */
    public Token.Type getOperator() {
        return operator;
    }
}
