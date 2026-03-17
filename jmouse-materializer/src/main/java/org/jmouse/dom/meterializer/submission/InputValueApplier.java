package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;

/**
 * {@link ControlValueApplier} for {@code <input>} elements. 🧩
 *
 * <p>
 * Applies submission values to input controls.
 * </p>
 *
 * <p>
 * Behavior depends on the input {@code type}:
 * </p>
 * <ul>
 *     <li>For {@code checkbox} and {@code radio}, the control is marked
 *     as {@code checked} when the node {@code value} matches the submitted value.</li>
 *     <li>For other input types, the value is assigned to the {@code value} attribute.</li>
 * </ul>
 */
public final class InputValueApplier implements ControlValueApplier {

    public static final String CHECKBOX = "checkbox";
    public static final String RADIO    = "radio";

    /**
     * Supports {@code <input>} nodes.
     */
    @Override
    public boolean supports(Node node) {
        return node.getTagName() == TagName.INPUT;
    }

    /**
     * Applies the provided value to the input control.
     */
    @Override
    public void apply(Node node, Object value) {
        String type      = node.getAttribute("type");
        String nodeValue = node.getAttribute("value");

        if (RADIO.equalsIgnoreCase(type) || CHECKBOX.equalsIgnoreCase(type)) {

            if (value instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (nodeValue != null && nodeValue.equals(String.valueOf(item))) {
                        node.addAttribute("checked", "checked");
                        return;
                    }
                }
                return;
            }

            String text = value == null ? "" : String.valueOf(value);

            if (nodeValue != null && nodeValue.equals(text)) {
                node.addAttribute("checked", "checked");
            }

            return;
        }

        String text = value == null ? "" : String.valueOf(value);

        node.addAttribute("value", text);
    }
}