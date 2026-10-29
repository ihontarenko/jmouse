package org.jmouse.cad.component;

import org.jmouse.cad.Given;
import org.jmouse.cad.Index;

/**
 * A figure a component shows — {@code display} — or an instrument reads — {@code measure} —: a value computed from the
 * part's nets and states, the unit it is shown in, and a caption.
 *
 * @param index   a figure for each slot of a row — {@code cell[32]} —, or null for one
 * @param unit    what it is shown in — {@code %}, {@code °C} —, or null for a bare number
 * @param signal  whether an instrument's reading is a signal to plot, rather than a number to show
 * @param caption what it says of itself; empty when not written
 */
public record Figure(String name, Index index, Given value, String unit, boolean signal, String caption) {

    public Figure {
        caption = caption == null ? "" : caption;
    }
}
