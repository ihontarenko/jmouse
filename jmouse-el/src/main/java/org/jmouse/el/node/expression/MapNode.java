package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a map literal node in the expression language.
 * <p>
 * This node aggregates key-value pairs from its children (expected to be instances of {@link KeyValueNode})
 * and evaluates to a {@link Map} containing the computed keys and values.
 * </p>
 */
public class MapNode extends AbstractExpressionNode {

    /**
     * Evaluates this map node by iterating over its child nodes and collecting key-value pairs.
     * <p>
     * Each child is expected to be a {@link KeyValueNode} that, when evaluated,
     * returns a {@link Map.Entry} containing the key and value.
     * </p>
     *
     * @param context the evaluation context
     * @return a Map containing the evaluated key-value pairs
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        Map<Object, Object> map = new HashMap<>();

        for (Node child : getChildren()) {
            if (child instanceof KeyValueNode kvNode) {
                Object result = kvNode.evaluate(context);
                if (result instanceof Map.Entry<?, ?> entry) {
                    map.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return map;
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
     * Returns a string representation of this map node.
     *
     * @return a string indicating the number of key-value pairs in the map
     */
    @Override
    public String toString() {
        return "MAP%s".formatted(getChildren());
    }
}
