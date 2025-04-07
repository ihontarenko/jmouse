package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

/**
 * Represents a raw text node in a templating system.
 *
 * <p>This node is used to store plain text that does not require further processing,
 * such as static HTML or text content in a template.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class TextNode extends AbstractNode {

    /**
     * The raw text content of this node.
     */
    private final char[] data;

    /**
     * Constructs a {@code RawTextNode} with the specified text content.
     *
     * @param string the raw text content
     */
    public TextNode(String string) {
        this.data = string.toCharArray();
    }

    /**
     * Returns the raw text content of this node.
     *
     * @return the raw text content
     */
    public String getString() {
        return new String(data);
    }

    /**
     * Recursively executes the given consumer on this node and all its children.
     *
     * @param visitor the consumer to execute on each node
     */
    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        }
    }

    @Override
    public String toString() {
        return "RAW[%s]".formatted(new String(data));
    }
}
