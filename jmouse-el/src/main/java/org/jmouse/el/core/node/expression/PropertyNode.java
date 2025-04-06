package org.jmouse.el.core.node.expression;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.node.AbstractExpressionNode;

/**
 * Represents a property expression node.
 * <p>
 * This node encapsulates a property path used to retrieve a value from the evaluation context.
 * When evaluated, it delegates to the context's {@code getValue} method using the stored property path.
 * </p>
 */
public class PropertyNode extends AbstractExpressionNode {

    private final String path;

    /**
     * Constructs a new PropertyNode with the specified property path.
     *
     * @param path the property path to be used for value retrieval
     */
    public PropertyNode(String path) {
        this.path = path;
    }

    /**
     * Returns the property path.
     *
     * @return the property path as a String
     */
    public String getPath() {
        return path;
    }

    /**
     * Evaluates this property node by retrieving the value associated with the property path
     * from the evaluation context.
     *
     * @param context the evaluation context providing value resolution
     * @return the value obtained from the context using the property path
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        return context.getValue(getPath());
    }

    /**
     * Returns a string representation of this property node.
     *
     * @return a formatted string indicating the property path
     */
    @Override
    public String toString() {
        return "PROPERTY_PATH['%s']".formatted(path);
    }
}
