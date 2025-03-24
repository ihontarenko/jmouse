package org.jmouse.template;

import org.jmouse.el.StringSource;

public class StandardTemplate implements Template {

    private final StringSource source;
    private Template           parent;

    public StandardTemplate(StringSource source) {
        this.source = source;
    }

    @Override
    public StringSource getSource() {
        return source;
    }

    @Override
    public String getName() {
        return source.getName();
    }

    @Override
    public void setParent(Template parent) {
        this.parent = parent;
    }

    @Override
    public Template getParent() {
        return parent;
    }

}
