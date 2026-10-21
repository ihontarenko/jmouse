package org.jmouse.kicad.footprint;

import java.util.Arrays;

/** The copper outline of a pad, as KiCad writes it. A custom pad's primitives are drawn as its anchor shape. */
public enum KiCadPadShape {

    CIRCLE("circle"),
    RECTANGLE("rect"),
    OVAL("oval"),
    TRAPEZOID("trapezoid"),
    ROUNDED_RECTANGLE("roundrect"),
    CUSTOM("custom");

    private final String word;

    KiCadPadShape(String word) {
        this.word = word;
    }

    public String word() {
        return word;
    }

    /**
     * The shape a word writes.
     *
     * @throws IllegalArgumentException naming the shapes KiCad has, for any other word
     */
    public static KiCadPadShape of(String word) {
        return Arrays.stream(values()).filter(shape -> shape.word.equals(word)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("'" + word + "' is no pad shape KiCad writes: "
                        + Arrays.stream(values()).map(KiCadPadShape::word).toList()));
    }
}
