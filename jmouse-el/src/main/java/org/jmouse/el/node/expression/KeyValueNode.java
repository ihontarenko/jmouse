package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Visitor;

import java.util.AbstractMap;

/**
 * Represents a key-value pair expression node.
 * <p>
 * This node evaluates its key and value expressions and returns them as a {@link java.util.Map.Entry}
 * (using {@link AbstractMap.SimpleEntry}). It is typically used within map literal expressions.
 * </p>
 */
public class KeyValueNode extends AbstractExpression {

    private Expression key;
    private Expression value;

    /**
     * Returns the expression representing the value.
     *
     * @return the value expression node
     */
    public Expression getValue() {
        return value;
    }

    /**
     * Sets the expression representing the value.
     *
     * @param value the value expression node to set
     */
    public void setValue(Expression value) {
        this.value = value;
    }

    /**
     * Returns the expression representing the key.
     *
     * @return the key expression node
     */
    public Expression getKey() {
        return key;
    }

    /**
     * Sets the expression representing the key.
     *
     * @param key the key expression node to set
     */
    public void setKey(Expression key) {
        this.key = key;
    }

    /**
     * Evaluates the key-value node by evaluating both key and value expressions.
     *
     * @param context the evaluation context
     * @return a {@link java.util.Map.Entry} containing the evaluated key and value
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        return new AbstractMap.SimpleEntry<>(key.evaluate(context), value.evaluate(context));
    }

    /**
     * Recursively executes the given consumer on this node and all its children.
     *
     * @param visitor the consumer to execute on each node
     */
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        key.accept(visitor);
        value.accept(visitor);
    }

    /**
     * Returns a string representation of the key-value pair.
     *
     * @return a string in the format "{key: value}"
     */
    @Override
    public String toString() {
        return "[%s : %s]".formatted(key, value);
    }
}
