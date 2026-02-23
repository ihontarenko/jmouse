package org.jmouse.dom;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Core DOM node abstraction used by the jMouse DOM model. 🌳
 *
 * <p>
 * Represents a tree element with parent/child relationships,
 * structural mutation operations, attributes, and traversal utilities.
 * </p>
 *
 * <h3>Tree structure</h3>
 * <ul>
 *     <li>Each node may have a parent</li>
 *     <li>Each node may have zero or more children</li>
 *     <li>Depth is managed explicitly via {@link #getDepth()} / {@link #setDepth(int)}</li>
 * </ul>
 *
 * <h3>Mutation operations</h3>
 * <ul>
 *     <li>{@link #append(Node)} / {@link #prepend(Node)}</li>
 *     <li>{@link #insertBefore(Node)} / {@link #insertAfter(Node)}</li>
 *     <li>{@link #wrap(Node)} / {@link #unwrap()}</li>
 *     <li>{@link #removeChild(Node)}</li>
 * </ul>
 *
 * <h3>Attributes</h3>
 * <p>
 * Element-like nodes may expose attributes via
 * {@link #addAttribute(String, String)}, {@link #getAttribute(String)},
 * and {@link #getAttributes()}.
 * </p>
 *
 * <p>
 * Implementations are responsible for maintaining structural consistency
 * (parent references, depth propagation, etc.).
 * </p>
 */
@SuppressWarnings({"unused"})
public interface Node {

    /**
     * Returns depth of this node in the tree.
     *
     * @return depth level (root typically 0)
     */
    int getDepth();

    /**
     * Sets depth of this node.
     *
     * @param depth new depth value
     */
    void setDepth(int depth);

    /**
     * Returns node type.
     *
     * @return node type
     */
    NodeType getNodeType();

    /**
     * Inserts child at the beginning of the children list.
     *
     * @param child node to prepend
     */
    void prepend(Node child);

    /**
     * Appends child to the end of the children list.
     *
     * @param child node to append
     */
    void append(Node child);

    /**
     * Removes given child node.
     *
     * @param child node to remove
     */
    void removeChild(Node child);

    /**
     * Returns list of children.
     *
     * @return children list (never null)
     */
    List<Node> getChildren();

    /**
     * @return {@code true} if node has children
     */
    default boolean hasChildren() {
        return getChildren() != null && !getChildren().isEmpty();
    }

    /**
     * Wraps this node inside the given wrapper node.
     *
     * @param wrapper wrapper node
     */
    void wrap(Node wrapper);

    /**
     * Removes this node from its wrapper (if applicable).
     */
    void unwrap();

    /**
     * Returns parent node.
     *
     * @return parent or {@code null}
     */
    Node getParent();

    /**
     * Sets parent node.
     *
     * @param node parent node
     */
    void setParent(Node node);

    /**
     * @return {@code true} if node has a parent
     */
    default boolean hasParent() {
        return getParent() != null;
    }

    /**
     * Inserts this node before the given sibling.
     *
     * @param sibling reference sibling
     */
    void insertBefore(Node sibling);

    /**
     * Inserts this node after the given sibling.
     *
     * @param sibling reference sibling
     */
    void insertAfter(Node sibling);

    /**
     * Returns list of sibling nodes.
     *
     * @return sibling list
     */
    List<Node> getSiblings();

    /**
     * Returns tag name for element nodes.
     *
     * @return tag name or {@code null} for non-element nodes
     */
    TagName getTagName();

    /**
     * Adds or replaces an attribute.
     *
     * @param key attribute name
     * @param value attribute value
     */
    void addAttribute(String key, String value);

    /**
     * Returns attribute value.
     *
     * @param name attribute name
     * @return attribute value or {@code null}
     */
    String getAttribute(String name);

    /**
     * Returns attribute map.
     *
     * @return attribute map
     */
    Map<String, String> getAttributes();

    /**
     * Traverses the node tree depth-first and executes given consumer.
     *
     * <p>
     * Uses {@link CopyOnWriteArrayList} snapshot to avoid
     * {@link java.util.ConcurrentModificationException}
     * during traversal.
     * </p>
     *
     * @param executor consumer applied to each node
     */
    default void execute(Consumer<Node> executor) {
        executor.accept(this);

        for (Node child : new CopyOnWriteArrayList<>(getChildren())) {
            child.execute(executor);
        }
    }

    /**
     * Adds CSS class to this node.
     *
     * @param className class to add
     */
    default void addClass(String className) {
        addClass(this, className);
    }

    /**
     * Checks whether this node contains the given CSS class.
     *
     * @param required class name
     * @return {@code true} if present
     */
    default boolean containsClass(String required) {
        return containsClass(getAttribute("class"), required);
    }

    /**
     * Adds CSS class to given node if not already present.
     *
     * @param node node
     * @param className class to add
     */
    static void addClass(Node node, String className) {
        String existing = node.getAttribute("class");

        if (existing == null || existing.isBlank()) {
            node.addAttribute("class", className);
            return;
        }

        if (containsClass(existing, className)) {
            return;
        }

        node.addAttribute("class", existing + " " + className);
    }

    /**
     * Checks if given class string contains required class token.
     *
     * @param classValue raw class attribute value
     * @param required required class token
     * @return {@code true} if found
     */
    static boolean containsClass(String classValue, String required) {
        String[] parts = classValue.trim().split("\\s+");

        for (String string : parts) {
            if (string.equals(required)) {
                return true;
            }
        }

        return false;
    }

}