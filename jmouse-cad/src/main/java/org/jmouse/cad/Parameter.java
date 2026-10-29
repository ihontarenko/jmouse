package org.jmouse.cad;

import java.util.Optional;
import java.util.Set;

/**
 * A parameter of a component or a circuit: its name, its value, and what a reader may set it to — a range, a step, a
 * unit it is counted per, and how it is offered ({@code log}, {@code integer}, {@code live}…).
 *
 * @param range       the values it may take, or null for any
 * @param step        how far one step of a control moves it, or null
 * @param per         the unit it is counted per — {@code V per unit} —, or null
 * @param flags       how it is offered, as written: {@code log}, {@code integer}, {@code exact}, {@code animate}, {@code live}
 * @param description what it is, for a reader; empty when not written
 */
public record Parameter(String name, Given value, Range range, Formula step, String per, Set<String> flags, String description) {

    public Parameter {
        flags = Set.copyOf(flags);
        description = description == null ? "" : description;
    }

    public Optional<Range> offered() {
        return Optional.ofNullable(range);
    }

    public boolean flagged(String flag) {
        return flags.contains(flag);
    }
}
