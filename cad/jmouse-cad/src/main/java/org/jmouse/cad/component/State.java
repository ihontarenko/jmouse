package org.jmouse.cad.component;

import org.jspecify.annotations.Nullable;
import org.jmouse.cad.Given;
import org.jmouse.cad.Index;
import org.jmouse.cad.Range;

/**
 * A state a component keeps between steps: its start, whether it changes in steps ({@code discrete}) or as a quantity
 * the solver integrates ({@code continuous}), what it may hold. A name with an index — {@code memory[256]} — is a row of
 * them.
 *
 * @param name        what it is called, as the equations name it
 * @param index       a row's slots — {@code memory[256]} —, or null for one state
 * @param start       what it holds at the start
 * @param kind        {@code discrete}: it changes in steps; {@code continuous}: the solver integrates it
 * @param range       the values it may hold, or null for any
 * @param description what it is, for a reader; empty when not written
 */
public record State(String name, @Nullable Index index, Given start, String kind, @Nullable Range range, String description) {

    public State {
        description = description == null ? "" : description;
    }

    public boolean continuous() {
        return kind.equals("continuous");
    }
}
