package org.jmouse.el.node;

import java.util.ArrayList;
import java.util.List;

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
        return !getChildren().isEmpty();
    }

    /**
     * Checks if the node has a parent.
     *
     * @return {@code true} if the node has a parent, otherwise {@code false}
     */
    default boolean hasParent() {
        return getParent() != null;
    }

    /**
     * Checks if the node is the sourceRoot (i.e., has no parent).
     *
     * @return {@code true} if the node is the sourceRoot, otherwise {@code false}
     */
    default boolean isRoot() {
        return !hasParent();
    }

    /**
     * Returns the parent of this node.
     *
     * @return the parent node or {@code null} if this is the sourceRoot
     */
    Node getParent();

    /**
     * Sets the parent node.
     *
     * @param node the parent node to set
     */
    void setParent(Node node);

    /**
     * Returns the list of child nodes.
     *
     * @return a list of child nodes
     */
    List<Node> getChildren();

    /**
     * Returns the list of child nodes.
     *
     * @return a list of child nodes
     */
    default <T> List<T> getChildren(Class<T> type) {
        List<T> result = new ArrayList<>();

        for (Node child : getChildren()) {
            if (type.isAssignableFrom(child.getClass())) {
                result.add(type.cast(child));
            }
        }

        return result;
    }

    /**
     * Returns the first child node.
     *
     * @return the first child node or {@code null} if no children exist
     */
    default Node getFirst() {
        return hasChildren() ? getChildren().getFirst() : null;
    }

    /**
     * Returns the last child node.
     *
     * @return the last child node or {@code null} if no children exist
     */
    default Node getLast() {
        return hasChildren() ? getChildren().getLast() : null;
    }

    /**
     * The comments and blank lines written immediately above this node.
     *
     * <p>⚠️ A parser is entitled to ignore trivia; a writer is not. See {@link Trivia}.</p>
     *
     * @return the leading trivia, in the order it was written; never {@code null}
     */
    default List<Trivia> getLeadingTrivia() {
        return List.of();
    }

    /**
     * The comment written after this node on its own line.
     *
     * @return it, or {@code null} where there was none
     */
    default Trivia getTrailingTrivia() {
        return null;
    }

    /**
     * Records what was written above this node.
     *
     * <p>Ignored by default: a node type that does not intend to survive a round trip does not have to
     * carry it, and every node built by hand rather than parsed has none.</p>
     *
     * @param trivia what was written
     */
    default void addLeadingTrivia(List<Trivia> trivia) {
    }

    /**
     * Records the comment written after this node on its line.
     *
     * @param trivia what was written
     */
    default void setTrailingTrivia(Trivia trivia) {
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
            node.setParent(this);
            getChildren().add(node);
        }
    }

    /**
     * Recursively executes the given consumer on this node and all its children.
     *
     * @param visitor the consumer to execute on each node
     */
    default void accept(Visitor visitor) {
        visitor.visit(this);
        if (hasChildren()) {
            for (Node child : getChildren()) {
                child.accept(visitor);
            }
        }
    }
}
