package org.jmouse.dom.meterializer.rules;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.meterializer.NodeRule;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.meterializer.RenderingExecution;
import org.jmouse.util.Strings;

import java.util.Map;
import java.util.Optional;

import static org.jmouse.core.Verify.nonNull;

/**
 * {@link NodeRule} that injects hidden {@code <input>} elements into a {@code <form>}.
 *
 * <p>
 * Reads a map from {@link RenderingExecution#request()} attribute and converts
 * entries into hidden inputs appended to the form.
 * </p>
 */
public final class AddHiddenInput implements NodeRule {

    /**
     * Request attribute name containing hidden input values.
     *
     * <p>Expected type: {@code Map<String,String>}.</p>
     */
    private final String attributeName;

    /**
     * Creates rule bound to a request attribute.
     *
     * @param attributeName request attribute containing hidden input map
     */
    public AddHiddenInput(String attributeName) {
        this.attributeName = nonNull(attributeName, "attributeName");
    }

    /**
     * Execution order among other {@link NodeRule}s.
     */
    @Override
    public int order() {
        return 110;
    }

    /**
     * Matches {@code <form>} elements.
     */
    @Override
    public boolean matches(Node node, RenderingExecution execution) {
        return node instanceof ElementNode elementNode && elementNode.getTagName() == TagName.FORM;
    }

    /**
     * Appends hidden inputs defined in the request attribute map.
     *
     * <p>
     * Each map entry becomes:
     * </p>
     *
     * <pre>{@code
     * <input type="hidden" name="key" value="value"/>
     * }</pre>
     */
    @Override
    @SuppressWarnings("unchecked")
    public void apply(Node node, RenderingExecution execution) {
        ElementNode form = (ElementNode) node;
        Optional<Object> optional = execution.request().findAttribute(attributeName);

        if (optional.isPresent() && optional.get() instanceof Map<?, ?> mapValue) {
            Map<String, String> hidden = (Map<String, String>) mapValue;

            for (Map.Entry<String, String> entry : hidden.entrySet()) {
                String name  = entry.getKey();
                String value = entry.getValue();

                if (Strings.isEmpty(name)) {
                    continue;
                }

                ElementNode input = new ElementNode(TagName.INPUT);

                input.addAttribute("type", "hidden");
                input.addAttribute("name", name);
                input.addAttribute("value", value == null ? "" : value);

                form.append(input);
            }
        }
    }
}