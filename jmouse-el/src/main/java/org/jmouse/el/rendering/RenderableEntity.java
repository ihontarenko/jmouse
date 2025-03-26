package org.jmouse.el.rendering;

import org.jmouse.el.StringSource;

import java.util.List;

public interface RenderableEntity {

    StringSource getSource();

    String getName();

    void setFragment(Fragment fragment);

    Fragment getFragment(String name);

    List<Fragment> getFragments();

    EntityStack getStack();

    default void setParent(RenderableEntity parent) {
        getStack().inherit(parent);
    }

    default RenderableEntity getParent() {
        return getStack().getParent();
    }

    default boolean hasParent() {
        return getParent() != null;
    }

}
