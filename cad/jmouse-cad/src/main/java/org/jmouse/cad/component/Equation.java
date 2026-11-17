package org.jmouse.cad.component;

import org.jspecify.annotations.Nullable;
import org.jmouse.cad.Formula;

/** A line of a component's {@code equations}, of a family's {@code formulas}, of a heat law: what it says, by its form. */
public sealed interface Equation {

    /**
     * {@code let V_be : voltage = …}: a name for a value the other equations use.
     *
     * @param name  the name it gives the value
     * @param type  the quantity it is — {@code voltage}, {@code ratio} —, or null
     * @param value the value, for jMF
     */
    record Local(String name, @Nullable String type, Formula value) implements Equation {
    }

    /**
     * {@code current base -> bi = …}, {@code voltage out -> 0 = …}: a branch through the part, from one pin or node to
     * another.
     *
     * @param kind  {@code current} or {@code voltage}
     * @param from  the pin or node it runs from
     * @param to    the pin or node it runs to
     * @param value what the current or voltage is, for jMF
     */
    record Branch(String kind, String from, String to, Formula value) implements Equation {
    }

    /**
     * {@code d(flux)/dt = primary - secondary}: how fast a state changes. A document writes it as any other
     * {@link Defined} line; it is a derivative because jMF reads its left side as one, where it is compiled.
     *
     * @param state the state that changes
     * @param rate  how fast it changes, for jMF
     */
    record Derivative(String state, Formula rate) implements Equation {
    }

    /**
     * {@code left = right}, both sides as written for jMF: a definition ({@code delay_scale(supply) = …}), a value
     * ({@code temperature = ambient + heating}), a next or a start value ({@code x_next = …}). {@code statement} is the
     * whole line as written: one statement of jMF, as jMF reads a definition.
     *
     * @param left      its left side, as written
     * @param right     its right side, as written
     * @param statement the whole line as written: one statement of jMF
     */
    record Defined(Formula left, Formula right, Formula statement) implements Equation {
    }
}
