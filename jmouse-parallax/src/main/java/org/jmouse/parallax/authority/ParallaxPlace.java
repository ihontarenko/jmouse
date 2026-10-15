package org.jmouse.parallax.authority;

import org.jmouse.parallax.owner.ParallaxOwner;

import java.util.ArrayList;
import java.util.List;

/**
 * Where something is, outermost first: nothing (the installation), or a chain of owners such as a workspace, the
 * project in it and the scene in that. A place names its whole chain, so the host decides a grant at any link
 * without looking a hierarchy up.
 */
public record ParallaxPlace(List<ParallaxOwner> chain) {

    private static final ParallaxPlace INSTALLATION = new ParallaxPlace(List.of());

    public ParallaxPlace {
        chain = List.copyOf(chain);
    }

    public static ParallaxPlace installation() {
        return INSTALLATION;
    }

    public static ParallaxPlace of(ParallaxOwner... chain) {
        return new ParallaxPlace(List.of(chain));
    }

    /** This place with one more link inside it. */
    public ParallaxPlace within(String type, String id) {
        List<ParallaxOwner> longer = new ArrayList<>(chain);
        longer.add(ParallaxOwner.of(type, id));
        return new ParallaxPlace(longer);
    }

    public boolean isInstallation() {
        return chain.isEmpty();
    }
}
