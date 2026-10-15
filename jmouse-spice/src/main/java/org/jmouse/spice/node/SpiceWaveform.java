package org.jmouse.spice.node;

import org.jmouse.spice.SpicePosition;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A source's transient function: {@code SIN(VO VA FREQ TD THETA PHASE)}, {@code PULSE(V1 V2 TD TR TF PW PER)},
 * {@code PWL(…)}, {@code EXP(…)}, {@code SFFM(…)}, {@code AM(…)} — its values in the order SPICE gives them, as
 * many as were written.
 */
public record SpiceWaveform(Kind kind, List<SpiceValue> values, SpicePosition position) {

    public SpiceWaveform {
        values = List.copyOf(values);
    }

    /** The transient functions SPICE sources know. */
    public enum Kind {
        SIN, PULSE, PWL, EXP, SFFM, AM;

        public static Optional<Kind> of(String word) {
            try {
                return Optional.of(valueOf(word.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException unknown) {
                return Optional.empty();
            }
        }
    }

    /** The value at a place, 0-based; empty when it was not written. */
    public Optional<SpiceValue> value(int index) {
        return index < values.size() ? Optional.of(values.get(index)) : Optional.empty();
    }

    /** {@code SIN(0 1 50)}, as SPICE writes it. */
    public String written() {
        return kind.name() + "(" + String.join(" ", values.stream().map(SpiceValue::written).toList()) + ")";
    }
}
