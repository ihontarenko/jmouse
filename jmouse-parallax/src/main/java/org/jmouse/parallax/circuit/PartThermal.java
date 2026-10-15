package org.jmouse.parallax.circuit;

import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * How a part warms and when it gives up, as its component writes it — the same parameters for every component, so a
 * new one in the catalogue warms and fails without a line of code:
 *
 * <pre>
 * thermal_resistance = 250 K/W        how many kelvin a watt raises it above the bench
 * thermal_time_constant = 10 s        how fast it gets there
 * T_maximum = 155 °C                  past this it fails, on a realistic bench
 * power_rating = 0.25 W               what it is rated for, shown beside what it takes
 * </pre>
 *
 * and in its {@code meta}, how it fails: {@code failure open} (a resistor, a fuse) or {@code failure short} (a junction).
 * A primitive part is read from the component of its kind ({@code resistor}, {@code capacitor} …); a part's own
 * override of one of these wins over its component's.
 *
 * @param resistance   K/W; 0 when the component says nothing — the part is then not warmed
 * @param timeConstant seconds
 * @param maximum      °C; NaN when the component sets no limit
 * @param rating       watts; NaN when the component states none
 * @param failure      {@code open} or {@code short}
 * @param coefficient  how its value moves per kelvin of its own heat, as a share — a resistor's
 *                     {@code temperature_coefficient}; 0 for a part that says nothing
 */
public record PartThermal(double resistance, double timeConstant, double maximum, double rating, String failure,
                          double coefficient) {

    static final String RESISTANCE = "thermal_resistance";
    static final String TIME_CONSTANT = "thermal_time_constant";
    static final String MAXIMUM = "T_maximum";
    static final String RATING = "power_rating";
    /** Written in ppm per kelvin, as a resistor writes it. */
    static final String COEFFICIENT = "temperature_coefficient";
    /** A part that says nothing of how fast it warms settles within a few seconds, as a small part on a board does. */
    static final double DEFAULT_TIME_CONSTANT = 5;

    /** Every part of a circuit whose component writes thermal data, by part id. */
    public static Map<String, PartThermal> of(CompiledObject object) {
        Map<String, PartThermal> thermals = new LinkedHashMap<>();
        if (!(object.definition() instanceof ObjectDefinition.Circuit circuit)) {
            return thermals;
        }
        for (CircuitElement element : circuit.elements()) {
            thermalOf(object, element).ifPresent(thermal -> thermals.put(element.id(), thermal));
        }
        return thermals;
    }

    private static Optional<PartThermal> thermalOf(CompiledObject object, CircuitElement element) {
        String reference = element.type() == CircuitElementType.MODEL ? element.component() : element.type().primitiveComponent();
        if (reference == null) {
            return Optional.empty();
        }
        ComponentModel model;
        try {
            model = ComponentDocuments.read(object.componentSource(reference), reference, object.library());
        } catch (IllegalArgumentException unreadable) {
            return Optional.empty();
        }
        Map<String, Double> values = new LinkedHashMap<>();
        model.parameters().forEach(parameter -> values.put(parameter.name(), parameter.value()));
        // A component writes its coefficient as a number of ppm; a part line's quantity is read in SI already.
        double coefficient = values.getOrDefault(COEFFICIENT, 0.0) * 1e-6;
        if (element.overrides() != null) {
            for (Map.Entry<String, String> override : element.overrides().entrySet()) {
                String name = override.getKey();
                if (values.containsKey(name)) {
                    try {
                        values.put(name, object.constant(override.getValue()));
                        if (name.equals(COEFFICIENT)) {
                            coefficient = values.get(name);
                        }
                    } catch (RuntimeException notConstant) {
                        // An override that is not a constant leaves the component's own figure.
                    }
                }
            }
        }
        double resistance = values.getOrDefault(RESISTANCE, 0.0);
        if (!(resistance > 0) && !values.containsKey(MAXIMUM)) {
            return Optional.empty();
        }
        return Optional.of(new PartThermal(Math.max(0, resistance), values.getOrDefault(TIME_CONSTANT, DEFAULT_TIME_CONSTANT),
                values.getOrDefault(MAXIMUM, Double.NaN), values.getOrDefault(RATING, Double.NaN),
                model.headerValue("failure").filter(failure -> failure.equals("short")).orElse("open"), coefficient));
    }

    /** The temperature it reaches after {@code seconds} at {@code power} from {@code from}, on a bench at {@code ambient}. */
    double warmed(double from, double power, double ambient, double seconds) {
        double settled = ambient + Math.max(0, power) * resistance;
        double time = timeConstant > 0 ? timeConstant : DEFAULT_TIME_CONSTANT;
        return settled + (from - settled) * Math.exp(-Math.max(0, seconds) / time);
    }
}
