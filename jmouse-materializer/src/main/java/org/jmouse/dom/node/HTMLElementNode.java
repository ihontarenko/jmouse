package org.jmouse.dom.node;

import org.jmouse.dom.TagName;

/**
 * Specialized {@link ElementNode} for HTML elements. 🌐
 *
 * <p>
 * Provides convenience methods for common HTML attributes
 * such as {@code id} and {@code class}.
 * </p>
 *
 * <p>
 * This class does not introduce new structural behavior —
 * it simply adds semantic helpers on top of {@link ElementNode}.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * HTMLElementNode div = new HTMLElementNode(TagName.DIV);
 * div.setId("main");
 * div.setClass("container fluid");
 * }</pre>
 */
public class HTMLElementNode extends ElementNode {

    /**
     * Creates a new HTML element node with the given tag name.
     *
     * @param tagName HTML tag name
     */
    public HTMLElementNode(TagName tagName) {
        super(tagName);
    }

    /**
     * Sets the {@code id} attribute.
     *
     * <p>
     * Equivalent to {@code addAttribute("id", id)}.
     * </p>
     *
     * @param id element id value
     */
    public void setId(String id) {
        addAttribute("id", id);
    }

    /**
     * Sets the {@code class} attribute.
     *
     * <p>
     * Replaces the existing {@code class} value.
     * To append a class token, use {@link org.jmouse.dom.Node#addClass(String)} instead.
     * </p>
     *
     * @param classNames space-separated class names
     */
    public void setClass(String classNames) {
        addAttribute("class", classNames);
    }

}