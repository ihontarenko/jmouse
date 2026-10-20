package org.jmouse.kicad.parser.node;

/**
 * Where something stands in a KiCad file: its 1-based line and column, as an editor counts them.
 *
 * @param line   the 1-based line; 0 for something a program built rather than read
 * @param column the 1-based column; 0 when only the line is known
 */
public record KiCadPosition(int line, int column) {

    /** The position of a node a program built rather than read. */
    public static final KiCadPosition NONE = new KiCadPosition(0, 0);

    @Override
    public String toString() {
        return column > 0 ? "line " + line + ":" + column : "line " + line;
    }
}
