package org.jmouse.template;

import org.jmouse.el.StringSource;

public interface Template {

    StringSource getSource();

    String getName();

    void setParent(Template parent);

    Template getParent();

    default boolean hasParent() {
        return getParent() != null;
    }

}
