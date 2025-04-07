package org.jmouse.el.node;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.util.helper.Strings.underscored;

/**
 * An abstract implementation of the {@link Node} interface, providing
 * basic parent-child relationships for hierarchical structures.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
abstract public class AbstractNode implements Node {

    protected final List<Node> children = new ArrayList<>(); // List of child nodes
    protected       Node       parent; // Parent node reference

    /**
     * Constructs an {@code AbstractNode} with no parent.
     */
    public AbstractNode() {
        this(null);
    }

    /**
     * Constructs an {@code AbstractNode} with the specified parent node.
     *
     * @param parent the parent node, or {@code null} if this is a root node
     */
    public AbstractNode(Node parent) {
        this.parent = parent;
    }

    /**
     * Returns the parent node of this node.
     *
     * @return the parent node or {@code null} if this is a root node
     */
    @Override
    public Node parent() {
        return this.parent;
    }

    /**
     * Sets the parent node of this node.
     *
     * @param node the parent node to set
     */
    @Override
    public void parent(Node node) {
        this.parent = node;
    }

    /**
     * Returns the list of child nodes.
     *
     * @return a list of child nodes
     */
    @Override
    public List<Node> children() {
        return this.children;
    }

    @Override
    public String toString() {
        return "%s".formatted(underscored(getClass().getSimpleName(), true));
    }
}
