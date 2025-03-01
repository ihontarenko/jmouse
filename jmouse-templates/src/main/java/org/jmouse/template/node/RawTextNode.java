package org.jmouse.template.node;

/**
 * Represents a raw text node in a templating system.
 *
 * <p>This node is used to store plain text that does not require further processing,
 * such as static HTML or text content in a template.</p>
 *
 * <pre>{@code
 * RawTextNode textNode = new RawTextNode("Hello, World!");
 * System.out.println(textNode.getString()); // Output: Hello, World!
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RawTextNode extends AbstractNode {

    /**
     * The raw text content of this node.
     */
    private final String string;

    /**
     * Constructs a {@code RawTextNode} with the specified text content.
     *
     * @param string the raw text content
     */
    public RawTextNode(String string) {
        this.string = string;
    }

    /**
     * Returns the raw text content of this node.
     *
     * @return the raw text content
     */
    public String getString() {
        return string;
    }
}
