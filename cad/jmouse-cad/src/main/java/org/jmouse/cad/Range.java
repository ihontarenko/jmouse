package org.jmouse.cad;

/** The values something may take: {@code low .. high}, or a list of them — {@code [60, 61]}, {@code ["write", "read"]}. */
public sealed interface Range {

    record Between(Formula low, Formula high) implements Range {
    }

    /** The list as written, brackets and all, for jMF to read. */
    record Listed(Formula values) implements Range {
    }
}
