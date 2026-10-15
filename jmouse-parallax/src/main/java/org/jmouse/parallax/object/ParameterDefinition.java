package org.jmouse.parallax.object;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A free symbol of an object's definition that a slider varies: its range, its step, whether the timeline may
 * animate it. Parameters are also the axes of the parameter space and of a parameter sweep.
 *
 * <p>Every number here is in SI, which is what a definition computes with. A slider is shown in the unit its value is
 * written in — {@code C = 1 µF in 1 nF .. 10 µF} moves in microfarads while the circuit receives farads — so the
 * author never multiplies by {@code 1e-9} to make a slider readable.</p>
 *
 * @param unit  the unit a slider shows its numbers in, prefix included ({@code µF}, {@code kΩ}, {@code V}); empty for
 *              a plain number
 * @param scale how many SI units one shown unit is: {@code 1e-6} for {@code µF}; a shown number times it is the value
 */
public record ParameterDefinition(String name, String label, double defaultValue, double minimum, double maximum,
                                  double step, boolean animatable, String unit, double scale, List<Double> values) {

    /** A plain number, shown as it is computed. */
    public ParameterDefinition(String name, String label, double defaultValue, double minimum, double maximum,
                               double step, boolean animatable) {
        this(name, label, defaultValue, minimum, maximum, step, animatable, "", 1);
    }

    /** A slider over its whole range, not over listed values. */
    public ParameterDefinition(String name, String label, double defaultValue, double minimum, double maximum,
                               double step, boolean animatable, String unit, double scale) {
        this(name, label, defaultValue, minimum, maximum, step, animatable, unit, scale, List.of());
    }

    private static final Pattern NAME = Pattern.compile("[A-Za-z\\p{IsGreek}][A-Za-z0-9_\\p{IsGreek}]{0,31}");

    public ParameterDefinition {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("A parameter name is a letter followed by letters, digits or '_'.");
        }
        if (label == null || label.isBlank()) {
            label = name;
        }
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || !(minimum < maximum)) {
            throw new IllegalArgumentException("Parameter '" + name + "' needs a range with minimum < maximum.");
        }
        if (!(defaultValue >= minimum && defaultValue <= maximum)) {
            throw new IllegalArgumentException("Parameter '" + name + "' has a default outside its range.");
        }
        if (!(step > 0)) {
            step = (maximum - minimum) / 100;
        }
        unit = unit == null ? "" : unit;
        values = values == null ? List.of() : List.copyOf(values);
        if (!(scale > 0) || !Double.isFinite(scale)) {
            scale = 1;
        }
    }

    public double clamp(double value) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    /** The value of every parameter: the given one when present and in range, the default otherwise. */
    public static double[] resolve(List<ParameterDefinition> parameters, Map<String, Double> values) {
        double[] resolved = new double[parameters.size()];
        for (int index = 0; index < parameters.size(); index++) {
            ParameterDefinition parameter = parameters.get(index);
            Double given = values == null ? null : values.get(parameter.name());
            if (given != null && !Double.isFinite(given)) {
                throw new IllegalArgumentException("Parameter '" + parameter.name() + "' must be a finite number.");
            }
            resolved[index] = given == null ? parameter.defaultValue() : parameter.clamp(given);
        }
        return resolved;
    }

    public static void requireKnown(List<ParameterDefinition> parameters, Map<String, Double> values) {
        if (values == null) {
            return;
        }
        Set<String> known = parameters.stream().map(ParameterDefinition::name).collect(Collectors.toSet());
        for (String name : values.keySet()) {
            if (!known.contains(name)) {
                throw new IllegalArgumentException("'" + name + "' is not a parameter of this object.");
            }
        }
    }
}
