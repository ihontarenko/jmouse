package org.jmouse.parallax.circuit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A metered part at a sample: the current into each of its terminals, the power it takes — negative for a source
 * delivering it —, and its states, by name.
 */
public record MeteredPart(Map<String, Double> terminals, double power, Map<String, Double> states) {

    public MeteredPart {
        states = states == null ? Map.of() : states;
    }

    public MeteredPart(Map<String, Double> terminals, double power) {
        this(terminals, power, Map.of());
    }

    /** Two pieces of one part, together: a part the cut split between two pieces of the circuit adds up. */
    MeteredPart plus(MeteredPart other) {
        Map<String, Double> sum = new LinkedHashMap<>(terminals);
        other.terminals.forEach((terminal, current) -> sum.merge(terminal, current, Double::sum));
        Map<String, Double> both = new LinkedHashMap<>(states);
        both.putAll(other.states);
        return new MeteredPart(sum, power + other.power, both);
    }
}
