package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Parameter;
import org.jmouse.cad.Range;
import org.jmouse.cad.Setting;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.simulation.ParameterEvaluation;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.cad.simulation.elaboration.ElaboratedCircuit;
import org.jmouse.cad.simulation.elaboration.ElaboratedPart;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a reader can work while a circuit runs: every {@code live} parameter of a part's component — a switch's
 * {@code closed}, a potentiometer's {@code position}. How each is worked follows from what it is: a whole number from 0
 * to 1 is a toggle, a button's is held, anything else is a slider.
 */
public final class LiveControls {

    /** How finely a slider moves when its parameter writes no step: a hundredth of its range. */
    private static final int STEPS = 100;

    /** How a control is worked. */
    public enum Kind { TOGGLE, MOMENTARY, SLIDER }

    /**
     * @param part      the part the control belongs to
     * @param parameter its component's parameter, which the part sets
     * @param value     its value now: the part's setting, else the component's own
     */
    public record LiveControl(String part, String parameter, String label, double minimum, double maximum, double step,
                              double value, Kind kind) {
    }

    private LiveControls() {
    }

    /** Every live control of the parts as written, in their order. */
    public static List<LiveControl> of(ElaboratedCircuit circuit) {
        List<LiveControl> controls = new ArrayList<>();
        for (ElaboratedPart part : circuit.written().parts()) {
            Map<String, Double> defaults = new LinkedHashMap<>();
            part.component().parameters().forEach(parameter -> defaults.put(parameter.name(), parameter.value()));
            boolean button = SimulationVocabulary.BUTTON.equals(part.component().type());
            for (Parameter parameter : liveParameters(part.component().lineage())) {
                control(part, parameter, defaults, button).ifPresent(controls::add);
            }
        }
        return controls;
    }

    /** The live parameters a lineage writes with a range to work in, the most particular writing of each. */
    private static List<Parameter> liveParameters(List<Component> lineage) {
        Map<String, Parameter> written = new LinkedHashMap<>();
        for (Component component : lineage) {
            component.parameters().forEach(parameter -> written.put(parameter.name(), parameter));
        }
        return written.values().stream()
                .filter(parameter -> parameter.flagged(SimulationVocabulary.LIVE) && parameter.range() instanceof Range.Between)
                .toList();
    }

    private static Optional<LiveControl> control(ElaboratedPart part, Parameter parameter, Map<String, Double> defaults,
                                                 boolean button) {
        String where = part.reference() + "." + parameter.name();
        Range.Between range = (Range.Between) parameter.range();
        double minimum;
        double maximum;
        double step;
        try {
            minimum = ParameterEvaluation.value(where, range.low(), defaults);
            maximum = ParameterEvaluation.value(where, range.high(), defaults);
            step = parameter.step() == null ? (maximum - minimum) / STEPS : ParameterEvaluation.value(where, parameter.step(), defaults);
        } catch (IllegalArgumentException unreadable) {
            return Optional.empty();
        }
        Optional<Setting> setting = part.settings().stream().filter(written -> written.name().equals(parameter.name())).findFirst();
        // A part line that drives the setting with the circuit's own parameter — `position = P` — makes that parameter's
        // slider the control; a second one here would sit at the component's default and move nothing.
        Optional<Double> set = setting.map(Setting::value).filter(Formula.class::isInstance).map(Formula.class::cast)
                .flatMap(formula -> Quantity.parse(formula.written()).map(Quantity::value));
        if (setting.isPresent() && set.isEmpty()) {
            return Optional.empty();
        }
        double value = set.orElseGet(() -> defaults.getOrDefault(parameter.name(), minimum));
        boolean whole = parameter.flagged(SimulationVocabulary.INTEGER) && minimum == 0 && maximum == 1;
        Kind kind = whole ? (button ? Kind.MOMENTARY : Kind.TOGGLE) : Kind.SLIDER;
        String label = parameter.description().isBlank() ? parameter.name() : parameter.description();
        return Optional.of(new LiveControl(part.reference(), parameter.name(), label, minimum, maximum, step > 0 ? step
                : (maximum - minimum) / STEPS, value, kind));
    }
}
