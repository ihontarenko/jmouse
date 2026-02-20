package org.jmouse.dom.node;

import org.jmouse.dom.TagName;

public class InputElementNode extends HTMLElementNode {

    public InputElementNode() {
        super(TagName.INPUT);
    }

    public void setName(String name) {
        addAttribute("name", name);
    }

    public void setType(String type) {
        addAttribute("type", type);
    }

    public void setValue(String value) {
        addAttribute("value", value);
    }

}
