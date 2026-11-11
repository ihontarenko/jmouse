package org.jmouse.cad.simulation;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Parameter;
import org.jmouse.cad.Range;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A document's parameters as a bench offers them: each its value worked out in SI, the range it moves in and its step,
 * and the unit it is written in — {@code C = 1 µF in 100 nF .. 10 µF step 100 nF} is a slider from 1e-7 to 1e-5, shown in
 * µF. What an interface draws a slider from; it never computes a formula itself.
 */
public final class ParameterSliders {

    /** How finely a slider moves when its parameter writes no step: a hundredth of its range. */
    private static final int STEPS = 100;

    /** The flag that lets a parameter move in time by itself, as an animation plays it. */
    private static final String ANIMATE = "animate";

    private ParameterSliders() {
    }

    /**
     * One parameter as a slider.
     *
     * @param label        what a reader calls it: its description, else its name
     * @param defaultValue its value as written, in SI
     * @param minimum      the low end of its range, in SI; the value itself when it writes none
     * @param maximum      the high end, in SI
     * @param unit         the unit its value is written in, prefix included ({@code µF}); empty for a plain number
     * @param scale        how many SI units one shown unit is ({@code µF}: 1e-6)
     */
    public record Slider(String name, String label, double defaultValue, double minimum, double maximum, double step,
                         boolean animatable, String unit, double scale) {
    }

    /**
     * Every parameter written as a number, in order; one written as words is no slider and is left out.
     *
     * @param title what a refusal names: the document
     * @throws IllegalArgumentException when a value or a bound cannot be computed
     */
    public static List<Slider> of(String title, List<Parameter> parameters) {
        Map<String, Double> known = new LinkedHashMap<>();
        for (EvaluatedParameter evaluated : ParameterEvaluation.evaluate(title, parameters)) {
            known.put(evaluated.name(), evaluated.value());
        }
        List<Slider> sliders = new ArrayList<>();
        for (Parameter parameter : parameters) {
            if (!(parameter.value() instanceof Formula written) || !known.containsKey(parameter.name())) {
                continue;
            }
            double value = known.get(parameter.name());
            Optional<Quantity> shown = Quantity.parse(written.written());
            String unit = shown.map(Quantity::writtenUnit).orElse("");
            double scale = shown.filter(quantity -> quantity.writtenNumber() != 0)
                    .map(quantity -> quantity.value() / quantity.writtenNumber()).orElse(1.0);
            // A slider moves between two bounds; a parameter that writes none stands still at its value.
            double minimum = value;
            double maximum = value;
            if (parameter.range() instanceof Range.Between between) {
                minimum = bound(title, between.low(), known);
                maximum = bound(title, between.high(), known);
            }
            double step = parameter.step() == null ? (maximum - minimum) / STEPS : bound(title, parameter.step(), known);
            String label = parameter.description().isBlank() ? parameter.name() : parameter.description();
            sliders.add(new Slider(parameter.name(), label, value, minimum, maximum, step, parameter.flagged(ANIMATE), unit,
                                   scale));
        }
        return sliders;
    }

    private static double bound(String title, Formula bound, Map<String, Double> known) {
        return ParameterEvaluation.value(title + ": a parameter's range", bound, known);
    }
}
