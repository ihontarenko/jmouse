package org.jmouse.template.rendering;

import org.jmouse.el.StringSource;

import java.util.List;

public interface RenderableEntity {

    StringSource getSource();

    String getName();

    void setParent(RenderableEntity parent);

    RenderableEntity getParent();

    void setFragment(Fragment fragment);

    Fragment getFragment(String name);

    List<Fragment> getFragments();

    default boolean hasParent() {
        return getParent() != null;
    }

}
