package org.jmouse.el.renderable;

import org.jmouse.el.core.rendering.Macro;

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
