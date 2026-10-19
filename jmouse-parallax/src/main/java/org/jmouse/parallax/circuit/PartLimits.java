package org.jmouse.parallax.circuit;

import org.jmouse.parallax.circuit.elaboration.ElaboratedCircuit;

import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;

import java.util.LinkedHashMap;
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
public record PartLimits(double maximum, double rating, ObjectDefinition.BenchConditions.Failure failure) {

    static final String MAXIMUM = "T_maximum";
    static final String RATING = "power_rating";

    /** Every part of a circuit whose component sets a limit or a rating, by part id, as the part stands on its bench. */
    static Map<String, PartLimits> of(ElaboratedCircuit circuit) {
        Map<String, PartLimits> limits = new LinkedHashMap<>();
        for (CircuitElement element : circuit.writtenParts()) {
            limitsOf(circuit, element).ifPresent(found -> limits.put(element.id(), found));
        }
        return limits;
    }

    private static Optional<PartLimits> limitsOf(ElaboratedCircuit circuit, CircuitElement element) {
        return circuit.componentOf(element).flatMap(model -> {
            Map<String, Double> values = circuit.parameters(model, element, "");
            if (!values.containsKey(MAXIMUM) && !values.containsKey(RATING)) {
                return Optional.empty();
            }
            return Optional.of(new PartLimits(values.getOrDefault(MAXIMUM, Double.NaN), values.getOrDefault(RATING, Double.NaN),
                    model.failure()));
        });
    }
}
