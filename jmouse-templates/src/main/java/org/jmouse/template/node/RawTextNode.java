package org.jmouse.template.node;

import org.jmouse.el.node.AbstractRenderableNode;

import java.util.Arrays;

/**
 * Represents a raw text node in a templating system.
 *
 * <p>This node is used to store plain text that does not require further processing,
 * such as static HTML or text content in a template.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RawTextNode extends AbstractRenderableNode {

    /**
     * The raw text content of this node.
     */
    private final char[] data;

    /**
     * Constructs a {@code RawTextNode} with the specified text content.
     *
     * @param string the raw text content
     */
    public RawTextNode(String string) {
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

    @Override
    public String toString() {
        return "RAW[%s]".formatted(new String(data));
    }
}
