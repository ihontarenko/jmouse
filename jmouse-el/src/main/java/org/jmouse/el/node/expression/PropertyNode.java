package org.jmouse.el.node.expression;

import org.jmouse.core.bind.PropertyPath;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.Visitor;

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
        PropertyPath         oldPath = PropertyPath.forPath(getPath());
        PropertyPath         newPath = PropertyPath.forPath(null);
        PropertyPath.Entries entries = oldPath.entries();
        int                  size    = entries.size();

        if (size > 1) {
            for (int i = 0; i < size; i++) {
                PropertyPath.Type type  = entries.type(i);
                String            value = entries.get(i).toString();

                if (type.isIndexed() && !type.isNumeric()) {
                    value = "[%s]".formatted(context.getValue(value));
                } else if (type.isNumeric()) {
                    value = "[%s]".formatted(value);
                }

                newPath = newPath.append(PropertyPath.forPath(value));
            }
        } else {
            newPath = oldPath;
        }

        return context.getValue(newPath.path());
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
     * Returns a string representation of this property node.
     *
     * @return a formatted string indicating the property path
     */
    @Override
    public String toString() {
        return "PROPERTY_PATH['%s']".formatted(path);
    }
}
