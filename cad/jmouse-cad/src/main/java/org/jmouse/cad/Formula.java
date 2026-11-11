package org.jmouse.cad;

import java.util.Objects;

/**
 * An expression as its author wrote it — {@code 4.7 kΩ}, {@code 100 * abs(anode.current) / I_maximum} —, for jMF to
 * compile where it is computed. The model says what it is for; what it means is the formula language's.
 */
public record Formula(String written) implements Given {

    public Formula {
        Objects.requireNonNull(written, "written");
        if (written.isBlank()) {
            throw new IllegalArgumentException("A formula writes an expression.");
        }
    }

    @Override
    public String toString() {
        return written;
    }
}
