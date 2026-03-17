package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;

/**
 * {@link ControlValueApplier} for boolean-like checkbox values. ☑️
 *
 * <p>
 * Applies the {@code checked} attribute to checkbox inputs when the
 * submitted value represents a logical {@code true}.
 * </p>
 *
 * <p>
 * Supported values include:
 * </p>
 * <ul>
 *     <li>{@link Boolean}</li>
 *     <li>{@link Number} ({@code 0} = false, non-zero = true)</li>
 *     <li>strings such as {@code true}, {@code false}, {@code 1}, {@code 0},
 *     {@code yes}, {@code no}, {@code on}, {@code off}</li>
 * </ul>
 *
 * <p>
 * This applier is intended for single boolean checkboxes rather than
 * checkbox groups with explicit {@code value} matching.
 * </p>
 */
public final class CheckboxBooleanApplier implements ControlValueApplier {

    /**
     * Supports {@code <input type="checkbox">} nodes.
     */
    @Override
    public boolean supports(Node node) {
        return node.getTagName() == TagName.INPUT
                && "checkbox".equalsIgnoreCase(node.getAttribute("type"));
    }

    /**
     * Marks the checkbox as checked when the value is truthy.
     */
    @Override
    public void apply(Node node, Object value) {
        if (isTruthy(value)) {
            node.addAttribute("checked", "checked");
        }
    }

    private boolean isTruthy(Object value) {
        switch (value) {
            case null -> {
                return false;
            }
            case Boolean booleanValue -> {
                return booleanValue;
            }
            case Number number -> {
                return number.intValue() != 0;
            }
            default -> {
            }
        }

        String text = String.valueOf(value).trim();

        if (text.isEmpty()) {
            return false;
        }

        return "true".equalsIgnoreCase(text)
                || "1".equals(text)
                || "yes".equalsIgnoreCase(text)
                || "on".equalsIgnoreCase(text);
    }
}