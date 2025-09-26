package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Visitor;

/**
 * Represents a literal expression node that encapsulates a constant value.
 * <p>
 * This abstract class serves as the base for literal expression nodes in the expression language.
 * It stores a constant value of type {@code T} and returns this value upon evaluation.
 * </p>
 *
 * @param <T> the type of the literal value
 */
abstract public class LiteralNode<T> extends AbstractExpression {

    private final T value;

    /**
     * Constructs a LiteralNode with the specified constant value.
     *
     * @param value the literal value to encapsulate; may be {@code null}
     */
    public LiteralNode(T value) {
        this.value = value;
    }

    /**
     * Returns the literal value encapsulated by this node.
     *
     * @return the literal value of type {@code T}
     */
    public T getValue() {
        return value;
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

    /**
     * Returns a string representation of the literal value.
     */
    @Override
    public String toString() {
        return value == null ? "NULL" : value.toString();
    }

    /**
     * Evaluates the literal node within the given evaluation context.
     * <p>
     * Since this node represents a constant literal, the evaluation simply returns the encapsulated value.
     * </p>
     *
     * @param context the evaluation context (ignored in this implementation)
     * @return the literal value
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        return value;
    }
}
