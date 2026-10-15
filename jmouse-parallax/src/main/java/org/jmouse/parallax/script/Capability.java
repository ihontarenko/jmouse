package org.jmouse.parallax.script;

import java.util.Arrays;
import java.util.Optional;

/**
 * What an object can do, and so which controls an interface shows for it. Derived from the document's structure by
 * {@link Capabilities} — never declared, because a declared capability can lie and a derived one cannot. An object
 * with none of them is static: it is computed once for the values of its parameters.
 */
public enum Capability {

    /** Time is an input: any frame is computed directly, so a time slider runs both ways. */
    PLAY("play"),
    /** Time evolves state: it runs forward, steps and resets, and goes back only from memory. */
    RUN("run"),
    /** Some values may change while it runs without resetting it. */
    INTERACT("interact"),
    /** It draws on randomness: a new sample is a new result. */
    RESAMPLE("resample"),
    /** It has nodes to measure with probes. */
    PROBE("probe"),
    /** A parameter with a range can be swept. */
    SWEEP("sweep"),
    /** It carries expectations to check. */
    CHECK("check"),
    /** It is a program with a {@code run} block. */
    EXECUTE("execute");

    private final String keyword;

    Capability(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() {
        return keyword;
    }

    public static Optional<Capability> ofKeyword(String word) {
        return Arrays.stream(values()).filter(capability -> capability.keyword.equals(word)).findFirst();
    }
}
