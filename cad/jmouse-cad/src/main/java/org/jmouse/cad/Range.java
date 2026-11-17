package org.jmouse.cad;

/** The values something may take: {@code low .. high}, or a list of them — {@code [60, 61]}, {@code ["write", "read"]}. */
public sealed interface Range {

    /**
     * {@code low .. high}: every value from one end to the other.
     *
     * @param low  its low end
     * @param high its high end
     */
    record Between(Formula low, Formula high) implements Range {
    }

    /**
     * The list as written, brackets and all, for jMF to read.
     *
     * @param values the values, as written
     */
    record Listed(Formula values) implements Range {
    }
}
