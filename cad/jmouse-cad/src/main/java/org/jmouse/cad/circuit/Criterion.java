package org.jmouse.cad.circuit;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Range;

/** What a measure is held to. */
public sealed interface Criterion {

    /** {@code near 6.86 kHz ± 5%}. */
    record Near(Formula value, Formula tolerance) implements Criterion {
    }

    /** {@code within 26 V .. 30 V}. */
    record Within(Range range) implements Criterion {
    }

    /** {@code is 1}, {@code is [1, 0, 1]}. */
    record Is(Formula value) implements Criterion {
    }
}
