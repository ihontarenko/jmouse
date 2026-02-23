package org.jmouse.dom.node;

import org.jmouse.dom.NodeType;
import org.jmouse.dom.TagName;

/**
 * Concrete DOM element node implementation. 🧱
 *
 * <p>
 * Represents a standard element node (e.g. {@code <div>}, {@code <input>}, {@code <span>}).
 * </p>
 *
 * <p>
 * Structural behavior (children handling, parent management, attributes,
 * depth propagation) is inherited from {@link AbstractNode}.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * ElementNode div = new ElementNode(TagName.DIV);
 * div.addAttribute("class", "container");
 * }</pre>
 *
 * <p>
 * The node type is always {@link NodeType#ELEMENT}.
 * </p>
 */
public class ElementNode extends AbstractNode {

    /**
     * Creates a new element node with the given tag name.
     *
     * @param tagName element tag name (must not be null)
     */
    public ElementNode(TagName tagName) {
        super(NodeType.ELEMENT, tagName);
    }

}