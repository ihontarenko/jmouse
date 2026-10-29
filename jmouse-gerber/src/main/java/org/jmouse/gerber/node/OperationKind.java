package org.jmouse.gerber.node;

import java.util.Arrays;
import java.util.Optional;

/** What an operation does at its point. */
public enum OperationKind {

    /** {@code D01}: draws to the point with the aperture, or, in a region, adds an edge to its contour. */
    DRAW("D01"),
    /** {@code D02}: moves to the point, drawing nothing; in a region, starts a contour. */
    MOVE("D02"),
    /** {@code D03}: stamps the aperture at the point. */
    FLASH("D03");

    private final String code;

    OperationKind(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<OperationKind> ofWord(String word) {
        return Arrays.stream(values()).filter(kind -> GerberWords.sameCode(word, kind.code)).findFirst();
    }
}
