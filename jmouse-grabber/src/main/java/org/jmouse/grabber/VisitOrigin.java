package org.jmouse.grabber;

import java.net.URI;

/**
 * 🧬 Where a visit came from.
 *
 * <p>Kept so a failure can be explained rather than merely reported: an address that 404s is much
 * easier to understand beside the page that linked to it and the route that decided to follow it.</p>
 *
 * @param address the page that discovered this one, or {@code null} for a seed
 * @param route   the route that was handling that page, or {@code null} for a seed
 */
public record VisitOrigin(URI address, String route) {

    private static final VisitOrigin SEED = new VisitOrigin(null, null);

    /**
     * 🌱 The origin of an address the caller supplied — nothing discovered it.
     */
    public static VisitOrigin seed() {
        return SEED;
    }

    /**
     * 🧬 Discovered on this page, by this route.
     */
    public static VisitOrigin discoveredOn(URI address, String route) {
        return new VisitOrigin(address, route);
    }

    public boolean isSeed() {
        return address == null;
    }

    @Override
    public String toString() {
        if (isSeed()) {
            return "seed";
        }

        return route == null ? address.toString() : address + " (" + route + ")";
    }

}
