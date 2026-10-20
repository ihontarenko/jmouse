package org.jmouse.kicad.parser;

import org.jmouse.kicad.parser.node.KiCadPosition;

/** A KiCad file that does not read, with where it stops reading. */
public final class KiCadParseException extends RuntimeException {

    private final KiCadPosition position;

    public KiCadParseException(String file, KiCadPosition position, String detail) {
        super(file + ", " + position + ": " + detail);
        this.position = position;
    }

    public KiCadPosition position() {
        return position;
    }
}
