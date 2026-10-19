package org.jmouse.parallax.circuit;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.script.ExperienceReader;
import org.jmouse.parallax.object.ComponentLibrary;
import org.jmouse.parallax.object.ObjectDefinition.Circuit;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElement;
import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.script.Quantity;
import org.jmouse.parallax.script.engine.ExperienceDefinitions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What a reader can work while a circuit runs: every {@code live} parameter of a part's component — a switch's
 * {@code closed}, a potentiometer's {@code position}. How each is worked follows from what it is: a whole number from 0
 * to 1 is a toggle, a button's is held, anything else is a slider.
 */
public final class LiveControls {

    /** How a control is worked. */
    public enum Kind { TOGGLE, MOMENTARY, SLIDER }

    /**
     * @param part      the part the control belongs to
     * @param parameter its component's parameter, which the part overrides
     * @param value     its value now: the part's override, else the component's default
     */
    public record LiveControl(String part, String parameter, String label, double minimum, double maximum, double step,
                              double value, Kind kind) {
    }

    private LiveControls() {
    }

    public static List<LiveControl> of(Circuit circuit, ComponentLibrary library) {
        List<LiveControl> controls = new ArrayList<>();
        for (CircuitElement element : circuit.elements()) {
            if (element.type() != CircuitElementType.MODEL) {
                continue;
            }
            Optional<ExperienceDocument> component = library.source(element.component()).flatMap(LiveControls::read);
            if (component.isEmpty()) {
                continue;
            }
            boolean button = component.get().type().filter("button"::equals).isPresent();
            for (Block.Parameter parameter : component.get().parameters()) {
                if (!parameter.has(Block.ParameterFlag.LIVE) || parameter.range().isEmpty()) {
                    continue;
                }
                ParameterDefinition slider;
                try {
                    slider = ExperienceDefinitions.slider(parameter);
                } catch (IllegalArgumentException unreadable) {
                    continue;
                }
                String overridden = element.overrides().get(parameter.name());
                // A part line that drives the setting with the circuit's own parameter — `position = P` — makes that
                // parameter's slider the control. Falling back to the component's default here put a second wiper on
                // the bench at 0.5, which moved nothing the reader could see.
                Optional<Double> set = Optional.ofNullable(overridden).flatMap(written -> Quantity.parse(written).map(Quantity::value));
                if (overridden != null && set.isEmpty()) {
                    continue;
                }
                double value = set.orElse(slider.defaultValue());
                double step = slider.step() > 0 ? slider.step() : (slider.maximum() - slider.minimum()) / 100;
                boolean whole = parameter.has(Block.ParameterFlag.INTEGER) && slider.minimum() == 0 && slider.maximum() == 1;
                Kind kind = whole ? (button ? Kind.MOMENTARY : Kind.TOGGLE) : Kind.SLIDER;
                String label = parameter.description().isBlank() ? parameter.name() : parameter.description();
                controls.add(new LiveControl(element.id(), parameter.name(), label, slider.minimum(), slider.maximum(), step,
                        value, kind));
            }
        }
        return controls;
    }

    private static Optional<ExperienceDocument> read(String source) {
        try {
            return Optional.of(ExperienceReader.readOne(source, "component"));
        } catch (ExperienceParseException unreadable) {
            return Optional.empty();
        }
    }
}
