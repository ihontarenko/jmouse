package org.jmouse.gerber;

import org.jmouse.el.parser.ParseException;

/**
 * A Gerber or an Excellon text that cannot be read: where, and what is wrong there.
 *
 * <p>Refusing is for what has no shape — a block without its {@code *}, an extended command never closed by
 * {@code %}, a coordinate that is not a number. A command the reader has no dedicated node for is not refused: it is
 * kept as written, so that writing the tree back gives the file back.</p>
 */
public class GerberParseException extends ParseException {

    private final GerberPosition position;
    private final String         detail;

    public GerberParseException(GerberPosition position, String detail) {
        super(position + ": " + detail);
        this.position = position;
        this.detail = detail;
    }

    public GerberPosition getPosition() {
        return position;
    }

    /** What is wrong, without the position in front of it. */
    public String getDetail() {
        return detail;
    }
}
