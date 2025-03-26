package org.jmouse.el.rendering;

public interface EntityStack {

    static EntityStack empty() {
        return new Inheritance();
    }

    void inherit(RenderableEntity template);

    void ascend();

    void descend();

    RenderableEntity getChild();

    RenderableEntity getParent();

    default boolean hasChild() {
        return getChild() != null;
    }

    default boolean hasParent() {
        return getParent() != null;
    }

}
