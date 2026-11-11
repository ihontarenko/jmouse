package org.jmouse.cad.simulation.elaboration;

import org.jmouse.cad.Setting;
import org.jmouse.cad.simulation.SimulatedFamily;
import org.jmouse.cad.simulation.SimulatedComponent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One part as it is simulated: the component it is, compiled, the net each of its pins is wired to, and what it sets
 * over its component's parameters — and what the bench sets over that.
 *
 * @param connections each wired pin's net, by the pin's name — a block's own pin as {@code A.output}, a pin the whole
 *                    package shares as itself —, in the order written
 * @param family      the logic family its digital pins follow, or null for a part with none
 * @param settings    what the part sets — {@code resistance: R}, {@code A.V_input_offset: 2 mV} —, in the order written
 * @param conditioned what the bench sets over its parameters, in SI: a tolerance of its own
 * @param reportedAs  what its circuit calls a pin, where that is not the pin's own name — a two-terminal part wired in pin
 *                    order is reached as {@code R1.positive} and {@code R1.negative} —: the name every reading of it is
 *                    reported under, so a sheet finds the current of a terminal by the name its wire ends on
 */
public record ElaboratedPart(String reference, SimulatedComponent component, Map<String, String> connections,
                             SimulatedFamily family, List<Setting> settings, Map<String, Double> conditioned,
                             Map<String, String> reportedAs) {

    public ElaboratedPart {
        connections = Collections.unmodifiableMap(new LinkedHashMap<>(connections));
        reportedAs = Map.copyOf(reportedAs);
        settings = List.copyOf(settings);
        conditioned = Collections.unmodifiableMap(new LinkedHashMap<>(conditioned));
    }
}
