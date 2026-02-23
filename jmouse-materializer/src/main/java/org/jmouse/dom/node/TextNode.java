package org.jmouse.dom.node;

import org.jmouse.dom.NodeType;

/**
 * DOM node representing plain text content. 📝
 *
 * <p>
 * A {@code TextNode} cannot contain child nodes and is rendered
 * as textual content inside its parent element.
 * </p>
 *
 * <p>
 * Escaping and formatting behavior are handled by the corresponding
 * renderer (e.g. {@code TextRenderer}).
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * TextNode text = new TextNode("Hello world");
 * }</pre>
 *
 * <p>
 * The node type is always {@link NodeType#TEXT}.
 * </p>
 */
public class TextNode extends NonElementNode {

    private String text;

    /**
     * Creates an empty text node.
     */
    public TextNode() {
        this(null);
    }

    /**
     * Creates a text node with the given value.
     *
     * @param text text content (may be null)
     */
    public TextNode(String text) {
        super(NodeType.TEXT);
        this.text = text;
    }

    /**
     * Returns text content.
     *
     * @return text value (may be null)
     */
    public String getText() {
        return text;
    }

    /**
     * Sets text content.
     *
     * @param text new text value
     */
    public void setText(String text) {
        this.text = text;
    }

}