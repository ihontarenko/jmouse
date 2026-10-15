package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.object.ObjectDefinition.CircuitElementType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The words a definition is written with that both directions must agree on: the constants an engine family keeps
 * in {@code parameters}, the names of the axes, the parts a circuit is built from and the setting that carries each
 * part's value. Writing and reading take them from here, so a word cannot drift between the two.
 */
final class DefinitionWords {

    static final String BAILOUT     = "bailout";
    static final String ITERATIONS  = "iterations";
    static final String TOLERANCE   = "tolerance";
    static final String DIMENSION   = "dimension";
    static final String ANGLE       = "angle";
    static final String GENERATIONS = "generations";
    static final String LABELS      = "labels";
    static final String WAVEFORM    = "waveform";
    static final String FREQUENCY   = "frequency";
    static final String MODEL_VALUE = "value";

    /** The axes of a box, in order: a vector field's, a hypersurface's, an IFS map's. */
    static final List<String> AXES = List.of("x", "y", "z", "w");

    private DefinitionWords() {
    }

    /** {@code KLEIN_BOTTLE} → {@code klein_bottle}: an enum constant as a jMX word. */
    static String word(Enum<?> constant) {
        return constant.name().toLowerCase(Locale.ROOT);
    }

    /** {@code klein_bottle} → {@code KLEIN_BOTTLE}, refused with the words the enum knows. */
    static <E extends Enum<E>> E constant(Class<E> type, String word, String what) {
        try {
            return Enum.valueOf(type, word.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException("'" + word + "' is not " + what + ": " + String.join(", ",
                    Arrays.stream(type.getEnumConstants()).map(DefinitionWords::word).toList()) + ".");
        }
    }

    /**
     * A two-terminal part a circuit writes in pin order — {@code R1 resistor (in, out) resistance = R} — and the
     * setting that carries its value.
     */
    enum Primitive {
        RESISTOR(CircuitElementType.RESISTOR, "resistor", "resistance"),
        CAPACITOR(CircuitElementType.CAPACITOR, "capacitor", "capacitance"),
        INDUCTOR(CircuitElementType.INDUCTOR, "inductor", "inductance"),
        DIODE(CircuitElementType.DIODE, "diode", "I_saturation"),
        VOLTAGE_SOURCE(CircuitElementType.VOLTAGE_SOURCE, "voltage-source", "U_output"),
        CURRENT_SOURCE(CircuitElementType.CURRENT_SOURCE, "current-source", "I_output");

        private final CircuitElementType type;
        private final String             reference;
        private final String             valueSetting;

        Primitive(CircuitElementType type, String reference, String valueSetting) {
            this.type = type;
            this.reference = reference;
            this.valueSetting = valueSetting;
        }

        CircuitElementType type() {
            return type;
        }

        /** The component the part is: the slug of its model in the catalogue. */
        String reference() {
            return reference;
        }

        /** The setting its value is written in: {@code resistance = R}. */
        String valueSetting() {
            return valueSetting;
        }

        static Primitive of(CircuitElementType type) {
            return Arrays.stream(values()).filter(primitive -> primitive.type == type).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(type + " is not a two-terminal part."));
        }

        static Optional<Primitive> ofReference(String reference) {
            return Arrays.stream(values()).filter(primitive -> primitive.reference.equals(reference)).findFirst();
        }
    }
}
