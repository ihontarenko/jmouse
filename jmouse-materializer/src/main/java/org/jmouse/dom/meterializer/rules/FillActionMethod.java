package org.jmouse.dom.meterializer.rules;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.dom.meterializer.NodeRule;
import org.jmouse.meterializer.RenderingExecution;
import org.jmouse.util.Strings;

import java.util.Map;

public final class FillActionMethod implements NodeRule {

    public static final String REQUEST_ATTRIBUTE = FillActionMethod.class.getName();

    private final String defaultMethod;

    public FillActionMethod(String defaultMethod) {
        this.defaultMethod = Verify.nonNull(defaultMethod, "defaultMethod");
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public boolean matches(Node node, RenderingExecution execution) {
        return node.getTagName() == TagName.FORM;
    }

    @Override
    public void apply(Node node, RenderingExecution execution) {
        ElementNode form      = (ElementNode) node;
        Object      attribute = execution.request().attributes().get(REQUEST_ATTRIBUTE);

        if (!(attribute instanceof RequestData formData)) {
            form.addAttribute("method", defaultMethod);
            return;
        }

        if (Strings.isNotEmpty(formData.action())) {
            form.addAttribute("action", formData.action());
        }

        String method = formData.method();

        if (Strings.isEmpty(method)) {
            method = defaultMethod;
        }

        form.addAttribute("method", method.toUpperCase());
    }

    public record RequestData(String action, String method, Map<String, String> hidden) {
        public RequestData {
            hidden = hidden == null ? Map.of() : Map.copyOf(hidden);
        }
    }
}