package org.jmouse.cad.pcb.drawing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How a pcb is made: its maker's profile, and what the pcb changes of it.
 *
 * @param profile the maker's profile it starts from, by its word
 * @param changes the values the pcb changes of its way of making, by rule word, in millimetres
 */
public record Rules(String profile, Map<String, Double> changes) {

    public Rules {
        changes = Collections.unmodifiableMap(new LinkedHashMap<>(changes));
    }
}
