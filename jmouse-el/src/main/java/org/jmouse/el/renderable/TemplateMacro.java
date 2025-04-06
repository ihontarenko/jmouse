package org.jmouse.el.renderable;

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
