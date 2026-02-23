package org.jmouse.dom.node;

import org.jmouse.dom.Node;
import org.jmouse.dom.NodeType;
import org.jmouse.dom.TagName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base class for non-element DOM nodes. 📄
 *
 * <p>
 * Represents nodes that cannot contain child nodes
 * (e.g. text, comment, CDATA).
 * </p>
 *
 * <p>
 * Structural mutation methods related to children
 * ({@link #append(Node)}, {@link #prepend(Node)})
 * are unsupported and will throw {@link UnsupportedOperationException}.
 * </p>
 *
 * <p>
 * Attribute support is technically available but typically unused
 * for non-element nodes.
 * </p>
 */
abstract public class NonElementNode extends AbstractNode {

    /**
     * Creates a non-element node of the given type.
     *
     * @param nodeType node type
     */
    public NonElementNode(NodeType nodeType) {
        super(nodeType, null);
    }

    /**
     * Non-element nodes cannot have children.
     *
     * @param child child node
     * @throws UnsupportedOperationException always
     */
    @Override
    public void prepend(Node child) {
        throw new UnsupportedOperationException(
                "Node [%s] cannot have child nodes".formatted(this)
        );
    }

    /**
     * Non-element nodes cannot have children.
     *
     * @param child child node
     * @throws UnsupportedOperationException always
     */
    @Override
    public void append(Node child) {
        throw new UnsupportedOperationException(
                "Node [%s] cannot have child nodes".formatted(this)
        );
    }

    /**
     * Removes a child from the internal list.
     *
     * <p>
     * Although non-element nodes should not have children,
     * this method removes from the internal collection for safety.
     * </p>
     *
     * @param child child node
     */
    @Override
    public void removeChild(Node child) {
        children.remove(child);
    }

    /**
     * Always returns an empty list.
     *
     * @return empty list
     */
    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
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
     * Non-element nodes typically do not use attributes,
     * but the capability is retained for flexibility.
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
     * Returns attribute map.
     *
     * @return attribute map
     */
    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

}