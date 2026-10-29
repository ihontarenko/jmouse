package org.jmouse.cad.component;

import java.util.Map;

/**
 * A footprint a component sits on, by slug, and which of its pins each pad is when the package numbers them its own
 * way — {@code 1 base} —; empty when the pads are the pins' own numbers.
 */
public record Seat(String footprint, Map<String, String> pads) {

    public Seat {
        pads = Map.copyOf(pads);
    }
}
