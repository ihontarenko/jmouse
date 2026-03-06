package org.jmouse.dom.meterializer.rules;

import org.jmouse.core.Verify;
import org.jmouse.dom.FormMetadata;
import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.dom.meterializer.NodeRule;
import org.jmouse.meterializer.RenderingExecution;
import org.jmouse.util.Strings;

import java.util.Map;

/**
 * {@link NodeRule} that enriches {@code <form>} elements using {@link FormMetadata}.
 *
 * <p>
 * Adds default method, resolves action/method attributes, and injects hidden inputs
 * defined in {@link FormMetadata}.
 * </p>
 */
public final class ComplementForm implements NodeRule {

    /**
     * Default HTTP method used when none is provided.
     */
    private final String defaultMethod;

    /**
     * Creates rule with the given default form method.
     *
     * @param defaultMethod fallback HTTP method (e.g. {@code POST})
     */
    public ComplementForm(String defaultMethod) {
        this.defaultMethod = Verify.nonNull(defaultMethod, "defaultMethod");
    }

    /**
     * Execution order among other {@link NodeRule}s.
     */
    @Override
    public int order() {
        return 100;
    }

    /**
     * Matches {@code <form>} nodes.
     */
    @Override
    public boolean matches(Node node, RenderingExecution execution) {
        return node.getTagName() == TagName.FORM;
    }

    /**
     * Applies form metadata to the node.
     *
     * <p>
     * Behavior:
     * </p>
     * <ul>
     *     <li>sets default method if no metadata is present</li>
     *     <li>applies action/method attributes</li>
     *     <li>adds hidden inputs from metadata</li>
     * </ul>
     */
    @Override
    public void apply(Node node, RenderingExecution execution) {
        ElementNode form      = (ElementNode) node;
        Object      attribute = execution.request().attributes().get(FormMetadata.REQUEST_ATTRIBUTE);

        if (!(attribute instanceof FormMetadata(String action, String method, Map<String, String> hidden))) {
            form.addAttribute("method", defaultMethod);
            return;
        }

        if (Strings.isNotEmpty(action)) {
            form.addAttribute("action", action);
        }

        if (Strings.isEmpty(method)) {
            method = defaultMethod;
        }

        form.addAttribute("method", method.toUpperCase());

        if (hidden != null) {
            hidden.forEach((name, value) -> {
                Node input = new ElementNode(TagName.INPUT);
                input.addAttribute("type", "hidden");
                input.addAttribute("name", name);
                input.addAttribute("value", value);
                form.append(input);
            });
        }
    }

}