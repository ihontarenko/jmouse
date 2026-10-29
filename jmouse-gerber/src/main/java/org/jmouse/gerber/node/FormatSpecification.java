package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

/**
 * {@code %FSLAX46Y46*%}: how a coordinate is written — leading zeros omitted, absolute, this many integer and decimal
 * digits. What a coordinate of an {@link Operation} means is read through it.
 */
public final class FormatSpecification extends GerberCommand {

    /** Six decimals in millimetres: a nanometre, what every board house reads. */
    public static final FormatSpecification NANOMETRES = new FormatSpecification(4, 6, GerberPosition.NONE);

    private final int integerDigits;
    private final int decimalDigits;

    public FormatSpecification(int integerDigits, int decimalDigits, GerberPosition position) {
        super(position);
        this.integerDigits = integerDigits;
        this.decimalDigits = decimalDigits;
    }

    public int integerDigits() {
        return integerDigits;
    }

    public int decimalDigits() {
        return decimalDigits;
    }

    /** A length as this format writes it: an integer of its last decimal. */
    public long coordinate(double length) {
        return Math.round(length * Math.pow(10, decimalDigits));
    }

    /** A coordinate as written, back to a length in the file's unit. */
    public double length(long coordinate) {
        return coordinate / Math.pow(10, decimalDigits);
    }

    @Override
    public void writeSource(SourceWriter writer) {
        extended(writer, body -> body.word("FSLA").word("X").number(integerDigits).number(decimalDigits)
                .word("Y").number(integerDigits).number(decimalDigits));
    }
}
