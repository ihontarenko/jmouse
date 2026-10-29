package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.simulation.elaboration.BenchConditions;
import org.jmouse.cad.simulation.elaboration.ElaboratedCircuit;
import org.jmouse.cad.simulation.elaboration.ElaboratedPart;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Where a part gives up, as its component writes it — the same lines for every component, so a new one in the catalogue
 * fails without a line of code:
 *
 * <pre>
 * T_maximum = 155 °C                  past this it fails, on a realistic bench
 * power_rating = 0.25 W               what it is rated for, shown beside what it takes
 * </pre>
 *
 * and in its {@code meta}, how it fails: {@code failure open} (a resistor, a fuse), {@code failure short} (a junction —
 * every pin together) or {@code failure short drain source} (the pins it names, the rest open).
 * How warm it is comes from its own {@code heat} law ({@link PartHeat}); this only says where that becomes too warm.
 *
 * @param maximum °C; NaN when the component sets no limit
 * @param rating  watts; NaN when the component states none
 * @param failure how it fails
 */
public record PartLimits(double maximum, double rating, BenchConditions.Failure failure) {

    static final String MAXIMUM = "T_maximum";
    static final String RATING = "power_rating";

    /** The meta field a component says how it fails in. */
    private static final String FAILURE = "failure";

    /** Every part of a circuit whose component sets a limit or a rating, by reference, as the part stands on its bench. */
    static Map<String, PartLimits> of(ElaboratedCircuit circuit) {
        Map<String, PartLimits> limits = new LinkedHashMap<>();
        for (ElaboratedPart part : circuit.written().parts()) {
            limitsOf(circuit, part).ifPresent(found -> limits.put(part.reference(), found));
        }
        return limits;
    }

    private static Optional<PartLimits> limitsOf(ElaboratedCircuit circuit, ElaboratedPart part) {
        Map<String, Double> values = circuit.parameters(part, "");
        if (!values.containsKey(MAXIMUM) && !values.containsKey(RATING)) {
            return Optional.empty();
        }
        return Optional.of(new PartLimits(values.getOrDefault(MAXIMUM, Double.NaN), values.getOrDefault(RATING, Double.NaN),
                failure(part.component())));
    }

    /**
     * How a component fails, as its {@code meta} writes it — {@code failure: short drain source}, its words the way
     * and then the pins —; open when it says nothing.
     */
    static BenchConditions.Failure failure(SimulatedComponent component) {
        return component.written().head().field(FAILURE).map(String::strip).filter(written -> !written.isEmpty())
                .map(written -> BenchConditions.Failure.of(List.of(written.split("\\s+")))).orElse(BenchConditions.Failure.OPEN);
    }
}
