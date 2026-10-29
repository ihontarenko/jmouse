package org.jmouse.cad.component;

import org.jmouse.cad.Given;
import org.jmouse.cad.Index;
import org.jmouse.cad.Range;

/**
 * A state a component keeps between steps: its start, whether it changes in steps ({@code discrete}) or as a quantity
 * the solver integrates ({@code continuous}), what it may hold. A name with an index — {@code memory[256]} — is a row of
 * them.
 *
 * @param index a row's slots — {@code memory[256]} —, or null for one state
 * @param range the values it may hold, or null for any
 */
public record State(String name, Index index, Given start, String kind, Range range, String description) {

    public State {
        description = description == null ? "" : description;
    }

    public boolean continuous() {
        return kind.equals("continuous");
    }
}
