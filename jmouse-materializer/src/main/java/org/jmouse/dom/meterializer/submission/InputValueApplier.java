package org.jmouse.dom.meterializer.submission;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;

public final class InputValueApplier implements ControlValueApplier {

    public static final String CHECKBOX = "checkbox";
    public static final String RADIO    = "radio";

    @Override
    public boolean supports(Node node) {
        return node.getTagName() == TagName.INPUT;
    }

    @Override
    public void apply(Node node, Object value) {
        String type = node.getAttribute("type");
        String text = value == null ? "" : String.valueOf(value);

        if (RADIO.equalsIgnoreCase(type) || CHECKBOX.equalsIgnoreCase(type)) {
            String nodeValue = node.getAttribute("value");
            if (nodeValue != null && nodeValue.equals(text)) {
                node.addAttribute("checked", "checked");
            }
            return;
        }

        node.addAttribute("value", text);
    }
}