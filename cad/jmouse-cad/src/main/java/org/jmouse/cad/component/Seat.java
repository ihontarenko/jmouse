package org.jmouse.cad.component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A footprint a component sits on, by slug, and which of its pins each pad is when the package numbers them its own
 * way — {@code 1 base} —; empty when the pads are the pins' own numbers. The pads keep the order they are written in, so
 * whatever is made of them — a netlist, a pcb file — reads the same every time.
 */
public record Seat(String footprint, Map<String, String> pads) {

    public Seat {
        pads = Collections.unmodifiableMap(new LinkedHashMap<>(pads));
    }
}
