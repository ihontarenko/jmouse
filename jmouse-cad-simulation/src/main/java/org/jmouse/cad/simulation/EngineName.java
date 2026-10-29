package org.jmouse.cad.simulation;

import java.util.Arrays;
import java.util.Optional;

/**
 * The names the engine gives every component, beside its own pins, states and parameters: what its equations, its heat
 * law and its instruments may read without declaring. They are the engine's contract, as a language's keywords are, so
 * a document may declare none of them itself.
 */
public enum EngineName {

    /** The part's temperature, kelvin: the bench's, raised on a realistic bench by the part's own heat. */
    TEMPERATURE("temperature"),
    /** 1 on a realistic bench, 0 on an ideal one. */
    REALISTIC("realistic"),
    /** Which corner of its spread a part stands at: −1, 0 or +1. */
    TOLERANCE_CORNER("tolerance_corner"),
    /** The bench's level of detail — 0 draft, 1 standard, 2 precise —; what it changes is the document's to say. */
    DETAIL("detail"),
    /** The circuit's time, seconds. */
    TIME("time"),
    /** The power the part takes, watts: what a heat law and an instrument read. */
    POWER("power"),
    /** 1 once the part has failed on a realistic bench, else 0: what a display or a symbol binding reads. */
    FAILED("failed");

    private final String written;

    EngineName(String written) {
        this.written = written;
    }

    /** As a document writes it. */
    public String written() {
        return written;
    }

    public static Optional<EngineName> of(String name) {
        return Arrays.stream(values()).filter(engineName -> engineName.written.equals(name)).findFirst();
    }
}
