package org.jmouse.gerber.node;

import java.util.Arrays;
import java.util.Optional;

/** How a draw runs from where the pen is to where it goes: straight, or along an arc one way or the other. */
public enum Interpolation {

    LINEAR("G01"),
    CLOCKWISE("G02"),
    COUNTERCLOCKWISE("G03");

    private final String code;

    Interpolation(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** The interpolation a word names — {@code G01}, or {@code G1} as older files write it. */
    public static Optional<Interpolation> ofWord(String word) {
        return Arrays.stream(values()).filter(interpolation -> GerberWords.sameCode(word, interpolation.code)).findFirst();
    }
}
