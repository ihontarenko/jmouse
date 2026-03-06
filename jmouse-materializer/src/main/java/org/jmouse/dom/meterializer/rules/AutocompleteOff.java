package org.jmouse.dom.meterializer.rules;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.meterializer.NodeRule;
import org.jmouse.meterializer.RenderingExecution;

/**
 * {@link NodeRule} that disables browser autocomplete for form fields.
 *
 * <p>
 * Traverses form descendants and adds {@code autocomplete="off"} to
 * {@code <input>} and {@code <textarea>} elements if the attribute
 * is not already present.
 * </p>
 */
public final class AutocompleteOff implements NodeRule {

    /**
     * Execution order among other {@link NodeRule}s.
     */
    @Override
    public int order() {
        return 200;
    }

    /**
     * Matches {@code <form>} elements.
     */
    @Override
    public boolean matches(Node node, RenderingExecution execution) {
        return node.getTagName() == TagName.FORM;
    }

    /**
     * Applies {@code autocomplete="off"} to input controls inside the form.
     */
    @Override
    public void apply(Node node, RenderingExecution execution) {
        node.execute(child -> {
            TagName tagName = child.getTagName();
            if (tagName == TagName.INPUT || tagName == TagName.TEXTAREA) {
                if (child.getAttribute("autocomplete") == null) {
                    child.addAttribute("autocomplete", "off");
                }
            }
        });
    }
}