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

public final class AddHiddenInput implements NodeRule {

    private final String attributeName;

    public AddHiddenInput(String attributeName) {
        this.attributeName = nonNull(attributeName, "attributeName");
    }

    @Override
    public int order() {
        return 110;
    }

    @Override
    public boolean matches(Node node, RenderingExecution execution) {
        return node instanceof ElementNode elementNode && elementNode.getTagName() == TagName.FORM;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void apply(Node node, RenderingExecution execution) {
        ElementNode      form     = (ElementNode) node;
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