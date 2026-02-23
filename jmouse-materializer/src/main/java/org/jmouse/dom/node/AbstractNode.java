package org.jmouse.dom.node;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.dom.NodeType;
import org.jmouse.dom.TagName;

import java.util.*;

/**
 * Base implementation of {@link Node} providing common DOM tree mechanics. 🧱
 *
 * <p>
 * {@code AbstractNode} implements:
 * </p>
 * <ul>
 *     <li>parent/children relationships</li>
 *     <li>depth tracking</li>
 *     <li>attribute storage (for element-like nodes)</li>
 *     <li>basic structural mutations: append/prepend, wrap/unwrap, insertBefore/insertAfter</li>
 *     <li>simple sibling access</li>
 * </ul>
 *
 * <p>
 * Implementations typically specialize only by choosing {@link NodeType} and {@link TagName}
 * and optionally adding node-specific payload (e.g. text/comment/cdata).
 * </p>
 *
 * <h3>Notes</h3>
 * <ul>
 *     <li>Depth is managed explicitly and updated for direct children on insertion.</li>
 *     <li>This implementation does not automatically re-depth entire subtrees when moving nodes.</li>
 *     <li>Children and attributes collections are mutable.</li>
 * </ul>
 */
abstract public class AbstractNode implements Node {

    /**
     * Shared validation message used for child-related operations.
     */
    public static final String CHILD_NODE_MUST_BE_NON_NULL = "Child node must be non-null";

    protected TagName             tagName;
    protected NodeType            nodeType;
    protected List<Node>          children   = new ArrayList<>();
    protected Map<String, String> attributes = new HashMap<>();
    protected int                 depth;
    protected Node                parent;

    /**
     * Creates a node with the given type and tag name.
     *
     * @param nodeType node type
     * @param tagName tag name (may be null for non-element nodes)
     */
    public AbstractNode(NodeType nodeType, TagName tagName) {
        this.tagName = tagName;
        this.depth = 0;
        this.nodeType = nodeType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDepth() {
        return depth;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * Prepends a child node.
     *
     * <p>
     * Also assigns parent reference and sets child depth to {@code this.depth + 1}.
     * </p>
     *
     * @param child child node
     */
    @Override
    public void prepend(Node child) {
        Verify.nonNull(child, CHILD_NODE_MUST_BE_NON_NULL);
        child.setDepth(this.depth + 1);
        child.setParent(this);
        children.addFirst(child);
    }

    /**
     * Appends a child node.
     *
     * <p>
     * Also assigns parent reference and sets child depth to {@code this.depth + 1}.
     * </p>
     *
     * @param child child node
     */
    @Override
    public void append(Node child) {
        Verify.nonNull(child, CHILD_NODE_MUST_BE_NON_NULL);
        child.setDepth(this.depth + 1);
        child.setParent(this);
        children.add(child);
    }

    /**
     * Removes the given child node from {@link #children}.
     *
     * <p>
     * This does not reset child's parent/depth.
     * </p>
     *
     * @param child child node
     */
    @Override
    public void removeChild(Node child) {
        Verify.nonNull(child, CHILD_NODE_MUST_BE_NON_NULL);
        children.remove(child);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Node> getChildren() {
        return children;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node getParent() {
        return parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParent(Node parent) {
        this.parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeType getNodeType() {
        return nodeType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TagName getTagName() {
        return tagName;
    }

    /**
     * Adds or replaces an attribute.
     *
     * <p>
     * No validation is performed; callers may enforce naming/value rules if needed.
     * </p>
     *
     * @param key attribute name
     * @param value attribute value
     */
    @Override
    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    /**
     * Returns an attribute value by name.
     *
     * @param name attribute name
     * @return attribute value or {@code null}
     */
    @Override
    public String getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Returns backing attributes map.
     *
     * @return mutable attributes map
     */
    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Wraps this node with a wrapper node.
     *
     * <p>
     * If this node has a parent, it is replaced by {@code wrapper} in the parent's children list,
     * then this node is appended into {@code wrapper}.
     * </p>
     *
     * <p>
     * Depth/parent handling:
     * </p>
     * <ul>
     *     <li>{@code wrapper.parent = parent}</li>
     *     <li>{@code wrapper.depth = parent.depth + 1}</li>
     *     <li>{@code this.parent = wrapper}</li>
     * </ul>
     *
     * @param wrapper wrapper node
     */
    @Override
    public void wrap(Node wrapper) {
        if (parent != null) {
            int index = parent.getChildren().indexOf(this);
            if (index != -1) {
                wrapper.setParent(parent);
                parent.getChildren().set(index, wrapper);
                wrapper.append(this);
                setParent(wrapper);
                wrapper.setDepth(parent.getDepth() + 1);
            }
        }
    }

    /**
     * Unwraps this node from its parent wrapper (if possible).
     *
     * <p>
     * If parent and grandparent exist, the parent wrapper is replaced by this node
     * in the grandparent's children list.
     * </p>
     *
     * <p>
     * Depth/parent handling:
     * </p>
     * <ul>
     *     <li>{@code this.parent = grandParent}</li>
     *     <li>{@code this.depth = grandParent.depth + 1}</li>
     * </ul>
     */
    @Override
    public void unwrap() {
        if (parent != null) {
            Node grandParent = parent.getParent();
            if (grandParent != null) {
                int index = grandParent.getChildren().indexOf(parent);
                if (index != -1) {
                    grandParent.getChildren().set(index, this);
                    setParent(grandParent);
                    setDepth(grandParent.getDepth() + 1);
                }
            }
        }
    }

    /**
     * Inserts {@code sibling} before this node under the same parent.
     *
     * <p>
     * If this node has no parent, nothing happens.
     * </p>
     *
     * @param sibling node to insert
     */
    @Override
    public void insertBefore(Node sibling) {
        if (parent != null) {
            int index = parent.getChildren().indexOf(this);
            if (index != -1) {
                parent.getChildren().add(index, sibling);
                sibling.setParent(parent);
                sibling.setDepth(parent.getDepth() + 1);
            }
        }
    }

    /**
     * Inserts {@code sibling} after this node under the same parent.
     *
     * <p>
     * If this node has no parent, nothing happens.
     * </p>
     *
     * @param sibling node to insert
     */
    @Override
    public void insertAfter(Node sibling) {
        if (parent != null) {
            int index = parent.getChildren().indexOf(this);
            if (index != -1 && index < parent.getChildren().size()) {
                parent.getChildren().add(index + 1, sibling);
                sibling.setParent(parent);
                sibling.setDepth(parent.getDepth() + 1);
            }
        }
    }

    /**
     * Returns sibling nodes adjacent to this node.
     *
     * <p>
     * The returned list has up to two slots:
     * </p>
     * <ul>
     *     <li>index 0: previous sibling or {@code null}</li>
     *     <li>index 1: next sibling or {@code null}</li>
     * </ul>
     *
     * <p>
     * If this node has no parent, an empty list is returned.
     * </p>
     *
     * @return list containing previous and next sibling references (nullable)
     */
    @Override
    public List<Node> getSiblings() {
        List<Node> siblings = new ArrayList<>();

        if (parent != null) {
            List<Node> parentChildren = parent.getChildren();
            int        currentIndex   = parentChildren.indexOf(this);

            if (currentIndex > 0) {
                siblings.add(parentChildren.get(currentIndex - 1));
            } else {
                siblings.add(null);
            }

            if (currentIndex < parentChildren.size() - 1) {
                siblings.add(parentChildren.get(currentIndex + 1));
            } else {
                siblings.add(null);
            }
        }

        return siblings;
    }

    /**
     * Returns a debug-friendly string representation.
     *
     * <p>
     * Format: {@code [TAG_]NODETYPE_NODE}, for example:
     * {@code DIV_ELEMENT_NODE} or {@code TEXT_NODE}.
     * </p>
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "%s%s_NODE".formatted(tagName == null ? "" : tagName.name().toUpperCase() + "_", nodeType);
    }
}