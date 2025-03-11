package org.jmouse.template.node.expression;

import org.jmouse.template.extension.Operator;
import org.jmouse.template.lexer.Token;
import org.jmouse.template.node.AbstractExpressionNode;
import org.jmouse.template.node.ExpressionNode;

/**
 * Represents a binary operation node in the Abstract Syntax Tree (AST).
 *
 * <p>A binary operation consists of two expressions (left and right) and an operator,
 * such as addition, subtraction, multiplication, or division.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class BinaryOperation extends AbstractExpressionNode {

    /**
     * The left-hand side expression of the binary operation.
     */
    private final ExpressionNode left;

    /**
     * The operator used in the binary operation.
     */
    private final Operator operator;

    /**
     * The right-hand side expression of the binary operation.
     */
    private final ExpressionNode right;

    /**
     * Constructs a {@code BinaryOperation} with the specified left-hand side expression,
     * operator, and right-hand side expression.
     *
     * @param left     the left-hand side expression of the binary operation
     * @param operator the operator used in the binary operation
     * @param right    the right-hand side expression of the binary operation
     */
    public BinaryOperation(ExpressionNode left, Operator operator, ExpressionNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    /**
     * Returns the left-hand side expression of the binary operation.
     *
     * @return the left-hand side expression
     */
    public ExpressionNode getLeft() {
        return left;
    }

    /**
     * Returns the right-hand side expression of the binary operation.
     *
     * @return the right-hand side expression
     */
    public ExpressionNode getRight() {
        return right;
    }

    /**
     * Returns the operator used in the binary operation.
     *
     * @return the operator
     */
    public Operator getOperator() {
        return operator;
    }
}
