package org.jmouse.spice;

import org.jmouse.el.parser.ParseException;

/**
 * A SPICE text that cannot be read: where, and what is wrong there.
 *
 * <p>Refusing is for what has no shape — a parenthesis that never closes, a {@code .subckt} without its
 * {@code .ends}, a resistor without a value. A card of a kind the reader has no dedicated node for is not refused: it
 * is read as a generic card and kept, so that a printer writes it back.</p>
 */
public class SpiceParseException extends ParseException {

    private final SpicePosition position;
    private final String        detail;

    public SpiceParseException(SpicePosition position, String detail) {
        super(position + ": " + detail);
        this.position = position;
        this.detail = detail;
    }

    public SpicePosition getPosition() {
        return position;
    }

    /** What is wrong, without the position in front of it. */
    public String getDetail() {
        return detail;
    }
}
