package org.jmouse.template.evaluation;

import org.jmouse.template.Template;

public interface TemplateInheritance {

    void inherit(Template template);

    void ascend();

    void descend();

    Template getChild();

    Template getParent();

    default boolean hasChild() {
        return getChild() != null;
    }

    default boolean hasParent() {
        return getParent() != null;
    }

}
