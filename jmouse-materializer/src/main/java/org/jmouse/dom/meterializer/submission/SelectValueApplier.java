package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;

import java.util.Collection;

/**
 * {@link ControlValueApplier} for {@code <select>} elements. 🧩
 *
 * <p>
 * Applies submitted values to {@code <option>} children by marking
 * matching options with the {@code selected} attribute.
 * </p>
 *
 * <p>
 * Supports both single values and collections:
 * </p>
 *
 * <pre>{@code
 * value = "admin"
 * value = ["admin", "user"]
 * }</pre>
 *
 * <p>
 * Each matching {@code <option value="...">} receives
 * {@code selected="selected"}.
 * </p>
 */
public final class SelectValueApplier implements ControlValueApplier {

    /**
     * Supports {@code <select>} nodes.
     */
    @Override
    public boolean supports(Node node) {
        return node.getTagName() == TagName.SELECT;
    }

    /**
     * Applies the submitted value to option children.
     */
    @Override
    public void apply(Node node, Object value) {
        if (value instanceof Collection<?> collection) {
            for (Object object : collection) {
                apply(node, object);
            }
            return;
        }

        String selected = value == null ? "" : String.valueOf(value);

        for (Node child : node.getChildren()) {
            if (child.getTagName() != TagName.OPTION) {
                continue;
            }

            String optionValue = child.getAttribute("value");

            if (optionValue != null && optionValue.equals(selected)) {
                child.addAttribute("selected", "selected");
            }
        }
    }
}