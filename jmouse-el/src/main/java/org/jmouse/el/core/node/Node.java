package org.jmouse.el.core.node;

import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a hierarchical node structure with parent-child relationships.
 * <p>
 * Nodes can have children and a single parent, allowing tree-like structures.
 * Provides utility methods to navigate and manipulate the node hierarchy.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@SuppressWarnings({"unused"})
public interface Node {

    /**
     * Checks if the node has children.
     *
     * @return {@code true} if the node has children, otherwise {@code false}
     */
    default boolean hasChildren() {
        return !children().isEmpty();
    }

    /**
     * Checks if the node has a parent.
     *
     * @return {@code true} if the node has a parent, otherwise {@code false}
     */
    default boolean hasParent() {
        return parent() != null;
    }

    /**
     * Checks if the node is the root (i.e., has no parent).
     *
     * @return {@code true} if the node is the root, otherwise {@code false}
     */
    default boolean isRoot() {
        return !hasParent();
    }

    /**
     * Returns the parent of this node.
     *
     * @return the parent node or {@code null} if this is the root
     */
    Node parent();

    /**
     * Sets the parent node.
     *
     * @param node the parent node to set
     */
    void parent(Node node);

    /**
     * Returns the list of child nodes.
     *
     * @return a list of child nodes
     */
    List<Node> children();

    /**
     * Returns the first child node.
     *
     * @return the first child node or {@code null} if no children exist
     */
    default Node first() {
        return hasChildren() ? children().getFirst() : null;
    }

    /**
     * Returns the last child node.
     *
     * @return the last child node or {@code null} if no children exist
     */
    default Node last() {
        return hasChildren() ? children().getLast() : null;
    }

    /**
     * Adds a child node to this node.
     * <p>
     * Ensures that the node is not added to itself.
     * </p>
     *
     * @param node the node to add as a child
     */
    default void add(Node node) {
        if (this != node) {
            node.parent(this);
            children().add(node);
        }
    }

    /**
     * Recursively executes the given consumer on this node and all its children.
     *
     * @param executor the consumer to execute on each node
     */
    default void execute(Consumer<Node> executor) {
        executor.accept(this);

        if (hasChildren()) {
            for (Node child : children()) {
                child.execute(executor);
            }
        }
    }
}
