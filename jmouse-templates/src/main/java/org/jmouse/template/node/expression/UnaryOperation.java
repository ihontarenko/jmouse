package org.jmouse.template.node.expression;

import org.jmouse.template.extension.Operator;
import org.jmouse.template.lexer.Token;
import org.jmouse.template.node.AbstractExpressionNode;
import org.jmouse.template.node.ExpressionNode;

/**
 * Represents a unary operation tag node in the Abstract Syntax Tree (AST).
 *
 * <p>This node is used to represent unary operations such as increment, decrement,
 * unary plus, or unary minus. It extends {@link AbstractExpressionNode} and holds a single operand
 * along with an operator represented by a {@link Operator}.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
abstract public class UnaryOperation extends AbstractExpressionNode {

    /**
     * The operator for the unary operation.
     *
     * <p>This field can represent operations such as increment, decrement, unary plus, or unary minus,
     * and is represented using a {@link Operator} value.</p>
     */
    private final Operator operator;

    /**
     * The operand on which the unary operation is applied.
     */
    private final ExpressionNode operand;

    /**
     * Constructs a {@code UnaryOperation} node with the specified operand and operator.
     *
     * @param operand  the operand tag on which the unary operation is applied
     * @param operator the token type representing the unary operator (e.g., increment, decrement, plus, minus)
     */
    public UnaryOperation(ExpressionNode operand, Operator operator) {
        this.operator = operator;
        this.operand = operand;
    }

    /**
     * Returns the operator for this unary operation.
     *
     * @return the {@link Operator} representing the operator
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * Returns the operand tag of this unary operation.
     *
     * @return the operand tag
     */
    public ExpressionNode getOperand() {
        return operand;
    }
}
