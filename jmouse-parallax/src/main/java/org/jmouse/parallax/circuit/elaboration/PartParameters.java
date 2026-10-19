package org.jmouse.parallax.circuit.elaboration;

import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.component.ComponentVocabulary;

import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * The parameters a part is read with — by its equations, its heat law, its instruments, its displays and its limits
 * alike, so none of them can read another part than the solver solves. In order: the bench's figures as a nominal
 * bench sets them ({@link ComponentVocabulary#nominalBench}), the component's own parameters, a primitive's written value
 * as its component's first parameter ({@code R1 resistor (a, b) 1k} is a resistance of 1 kΩ), then what the part
 * overrides — the bench's figures among them, which the bench writes on every part it conditions.
 */
public final class PartParameters {

    private PartParameters() {
    }

    /**
     * One unit's parameters: an override written {@code Vos} is every unit's, {@code A.Vos} unit {@code A}'s only.
     *
     * @param unit    the unit, or "" for the part as a whole — which reads no unit's own override
     * @param reading how a written value is read: a quantity or an expression of the circuit's parameters, or of the
     *                parameters of the subcircuit the part is inside
     * @throws IllegalArgumentException for an override of a parameter the component does not have
     */
    public static Map<String, Double> resolve(ComponentModel model, CircuitElement element, String unit, ToDoubleFunction<String> reading) {
        Map<String, Double> values = ComponentVocabulary.nominalBench();
        model.parameters().forEach(parameter -> values.put(parameter.name(), parameter.value()));
        boolean primitive = element.type() != CircuitElementType.MODEL;
        if (primitive && !model.parameters().isEmpty() && element.value() != null && !element.value().isBlank()) {
            values.put(model.parameters().getFirst().name(), reading.applyAsDouble(element.value()));
        }
        if (element.overrides() == null) {
            return values;
        }
        element.overrides().forEach((key, expression) -> {
            int dot = key.indexOf('.');
            String name = dot < 0 ? key : key.substring(dot + 1);
            if (!values.containsKey(name)) {
                throw new IllegalArgumentException("'" + element.id() + "': " + model.name() + " has no parameter '" + name
                        + "' to override.");
            }
            if (dot < 0 || key.startsWith(unit + ".") && !unit.isEmpty()) {
                values.put(name, read(element, name, expression, reading));
            }
        });
        return values;
    }

    private static double read(CircuitElement element, String name, String expression, ToDoubleFunction<String> reading) {
        try {
            return reading.applyAsDouble(expression);
        } catch (RuntimeException unreadable) {
            throw new IllegalArgumentException(element.id() + ": its " + name + " = '" + expression
                    + "' is neither a value nor an expression of the circuit's parameters.", unreadable);
        }
    }
}
