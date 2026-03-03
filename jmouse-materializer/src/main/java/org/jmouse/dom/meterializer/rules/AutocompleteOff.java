package org.jmouse.dom.meterializer.rules;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.meterializer.NodeRule;
import org.jmouse.meterializer.RenderingExecution;

public final class AutocompleteOff implements NodeRule {

    @Override
    public int order() {
        return 200;
    }

    @Override
    public boolean matches(Node node, RenderingExecution execution) {
        return node.getTagName() == TagName.FORM;
    }

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