package org.jmouse.cad.circuit;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Range;

/** What a measure is held to. */
public sealed interface Criterion {

    /**
     * {@code near 6.86 kHz ± 5%}.
     *
     * @param value     the value the measure is held near
     * @param tolerance how far from it the measure may be: {@code 5%}, {@code 2 mV}
     */
    record Near(Formula value, Formula tolerance) implements Criterion {
    }

    /**
     * {@code within 26 V .. 30 V}.
     *
     * @param range the values the measure may take
     */
    record Within(Range range) implements Criterion {
    }

    /**
     * {@code is 1}, {@code is [1, 0, 1]}.
     *
     * @param value the value — or the row of values — the measure must be
     */
    record Is(Formula value) implements Criterion {
    }
}
