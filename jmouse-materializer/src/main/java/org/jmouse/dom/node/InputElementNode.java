package org.jmouse.dom.node;

import org.jmouse.dom.TagName;

/**
 * Specialized HTML element node representing {@code <input>} elements. ⌨️
 *
 * <p>
 * Provides convenience setters for common input attributes such as
 * {@code name}, {@code type}, and {@code value}.
 * </p>
 *
 * <p>
 * The tag name is fixed to {@link TagName#INPUT}.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * InputElementNode input = new InputElementNode();
 * input.setType("text");
 * input.setName("username");
 * input.setValue("john");
 * }</pre>
 *
 * <p>
 * This class does not introduce additional structural behavior —
 * it is a convenience wrapper over {@link HTMLElementNode}.
 * </p>
 */
public class InputElementNode extends HTMLElementNode {

    /**
     * Creates a new {@code <input>} element node.
     */
    public InputElementNode() {
        super(TagName.INPUT);
    }

    /**
     * Sets the {@code name} attribute.
     *
     * @param name input name
     */
    public void setName(String name) {
        addAttribute("name", name);
    }

    /**
     * Sets the {@code type} attribute.
     *
     * @param type input type (e.g. {@code text}, {@code password}, {@code checkbox})
     */
    public void setType(String type) {
        addAttribute("type", type);
    }

    /**
     * Sets the {@code value} attribute.
     *
     * @param value input value
     */
    public void setValue(String value) {
        addAttribute("value", value);
    }

}