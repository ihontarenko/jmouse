package org.jmouse.el.node.expression;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.UnaryOperator;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Visitor;

/**
 * Represents a unary operation tag node in the Abstract Syntax Tree (AST).
 *
 * <p>This node is used to represent unary operations such as increment, decrement,
 * unary plus, or unary minus. It extends {@link AbstractExpression} and holds a single operand
 * along with an operator represented by a {@link Operator}.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
abstract public class UnaryOperation extends AbstractExpression {

    /**
     * The operator for the unary operation.
     *
     * <p>This field can represent operations such as increment, decrement, unary plus, or unary minus,
     * and is represented using a {@link Operator} value.</p>
     */
    protected final Operator operator;

    /**
     * The operand on which the unary operation is applied.
     */
    protected final Expression operand;

    /**
     * Constructs a {@code UnaryOperation} node with the specified operand and operator.
     *
     * @param operand  the operand tag on which the unary operation is applied
     * @param operator the token type representing the unary operator (e.g., increment, decrement, plus, minus)
     */
    public UnaryOperation(Expression operand, Operator operator) {
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
    /**
     * The value a step operator starts from: a variable never assigned counts from zero.
     *
     * <p>{@code taken++} on a name that does not exist yet is the first line of every counter, and it
     * used to fail on the missing value's type. Only {@code ++} and {@code --} on a variable get the
     * zero; every other operator, and every operand that is not a variable, sees the value as it is.</p>
     *
     * @param operand  what the operator applies to
     * @param operator the operator
     * @param value    the operand's evaluated value
     * @return {@code 0} for an unassigned variable under {@code ++} or {@code --}; {@code value} otherwise
     */
    public static Object countingFromZero(Expression operand, Operator operator, Object value) {
        boolean step = operator == UnaryOperator.INCREMENT || operator == UnaryOperator.DECREMENT;
        return value == null && step && operand instanceof PropertyNode ? Integer.valueOf(0) : value;
    }

    public Expression getOperand() {
        return operand;
    }

    /**
     * Recursively executes the given consumer on this node and all its children.
     *
     * @param visitor the consumer to execute on each node
     */
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}
