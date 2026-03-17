package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;

import java.util.Collection;

public final class SelectValueApplier implements ControlValueApplier {

    @Override
    public boolean supports(Node node) {
        return node.getTagName() == TagName.SELECT;
    }

    @Override
    public void apply(Node node, Object value) {
        if (value instanceof Collection<?> collection) {
            for (Object object : collection) {
                apply(node, object);
            }
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