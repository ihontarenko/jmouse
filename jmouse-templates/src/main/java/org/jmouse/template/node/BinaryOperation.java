package org.jmouse.template.node;

import org.jmouse.template.lexer.Token;

/**
 * Represents a binary operation node in the Abstract Syntax Tree (AST).
 * <p>
 * A binary operation combines two subexpressions (the left-hand side and the right-hand side)
 * with an operator. In this implementation, the left-hand and right-hand expressions are stored
 * explicitly, while the operator information is passed to the superclass constructor.
 * </p>
 *
 * <p>
 * Example binary operations include addition, subtraction, multiplication, division, etc.
 * </p>
 *
 * @author Ivan Hontarenko
 * @version 1.0
 */
public class BinaryOperation extends AbstractExpression {

    private final Expression left;
    private final Expression right;

    /**
     * Constructs a BinaryOperation node with the specified left-hand side expression,
     * operator (passed to the superclass), and right-hand side expression.
     * <p>
     * Note: The operator information is encapsulated in the parameter passed to the superclass constructor.
     * If required, the implementation can be extended to store the operator separately.
     * </p>
     *
     * @param left    the left-hand side expression of the binary operation
     * @param operand the expression representing the operator; this value is passed to the superclass constructor
     * @param right   the right-hand side expression of the binary operation
     */
    public BinaryOperation(Expression left, Expression operand, Expression right) {
        super(operand);
        this.left = left;
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
}
