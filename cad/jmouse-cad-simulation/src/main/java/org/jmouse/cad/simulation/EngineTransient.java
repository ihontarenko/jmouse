package org.jmouse.cad.simulation;

import java.util.List;
import java.util.Map;

/**
 * What another engine computed of a circuit's transient, in the circuit's own names.
 *
 * @param voltages each net's voltage
 * @param currents each source's current
 * @param given    what the engine ran, in its own words — a SPICE deck
 * @param notes    what it could not be told
 */
public record EngineTransient(double[] time, Map<String, double[]> voltages, Map<String, double[]> currents, String given,
                              List<String> notes) {

    public EngineTransient {
        voltages = Map.copyOf(voltages);
        currents = Map.copyOf(currents);
        notes = List.copyOf(notes == null ? List.of() : notes);
    }
}
