package org.jmouse.dom.meterializer;

/**
 * Enumeration of supported HTML {@code <input>} types. ⌨️
 *
 * <p>
 * Each enum constant maps to a concrete HTML {@code type} attribute value.
 * This abstraction improves type-safety when generating input elements
 * programmatically.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * InputElementNode input = new InputElementNode();
 * input.setType(InputType.EMAIL.htmlValue());
 * input.setName("userEmail");
 * }</pre>
 *
 * <p>
 * Using this enum avoids hard-coded string literals such as
 * {@code "text"} or {@code "password"} scattered across code.
 * </p>
 */
public enum InputType {

    /**
     * Standard single-line text input.
     */
    TEXT("text"),

    /**
     * Numeric input (browser may enforce numeric UI).
     */
    NUMBER("number"),

    /**
     * Email input with browser-level validation.
     */
    EMAIL("email"),

    /**
     * Password input (masked characters).
     */
    PASSWORD("password"),

    /**
     * Date picker input.
     */
    DATE("date"),

    /**
     * URL input with browser-level validation.
     */
    URL("url"),

    /**
     * Generic button (no submission by default).
     */
    BUTTON("button"),

    /**
     * Submit button.
     */
    SUBMIT("submit");

    private final String htmlValue;

    InputType(String htmlValue) {
        this.htmlValue = htmlValue;
    }

    /**
     * Returns the HTML {@code type} attribute value.
     *
     * @return string value used in {@code <input type="...">}
     */
    public String htmlValue() {
        return htmlValue;
    }
}