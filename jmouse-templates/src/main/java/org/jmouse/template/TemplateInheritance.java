package org.jmouse.template;

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
