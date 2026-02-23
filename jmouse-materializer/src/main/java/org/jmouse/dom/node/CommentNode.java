package org.jmouse.dom.node;

import org.jmouse.dom.NodeType;

/**
 * DOM node representing an HTML/XML comment. 💬
 *
 * <p>
 * Produces markup in the form:
 * </p>
 *
 * <pre>{@code
 * <!-- comment text -->
 * }</pre>
 *
 * <p>
 * Internally delegates text storage to a {@link TextNode}
 * to reuse text handling logic.
 * </p>
 *
 * <p>
 * The node type is always {@link NodeType#COMMENT}.
 * </p>
 */
public class CommentNode extends AbstractNode {

    private final TextNode comment = new TextNode();

    /**
     * Creates a comment node with the given text.
     *
     * @param comment comment text (may be null)
     */
    public CommentNode(String comment) {
        super(NodeType.COMMENT, null);
        this.comment.setText(comment);
    }

    /**
     * Creates an empty comment node.
     */
    public CommentNode() {
        this(null);
    }

    /**
     * Returns comment text.
     *
     * @return comment text (may be null)
     */
    public String getComment() {
        return comment.getText();
    }

    /**
     * Sets comment text.
     *
     * @param comment new comment text
     */
    public void setComment(String comment) {
        this.comment.setText(comment);
    }

}