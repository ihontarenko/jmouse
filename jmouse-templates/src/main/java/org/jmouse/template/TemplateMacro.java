package org.jmouse.template;

import org.jmouse.el.rendering.Macro;

public class TemplateMacro implements Macro {

    private final String name;

    public TemplateMacro(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
