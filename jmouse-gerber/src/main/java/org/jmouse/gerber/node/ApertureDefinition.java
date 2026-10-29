package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;
import org.jmouse.gerber.GerberShape;

/** {@code %ADD10C,0.25*%}: an aperture, its D code and its shape. */
public final class ApertureDefinition extends GerberCommand {

    /** The first D code an aperture may take; D00–D09 are the format's own. */
    public static final int FIRST_CODE = 10;

    private final int         code;
    private final GerberShape shape;

    public ApertureDefinition(int code, GerberShape shape, GerberPosition position) {
        super(position);
        if (code < FIRST_CODE) {
            throw new IllegalArgumentException("An aperture's D code is " + FIRST_CODE + " or more: " + code);
        }
        this.code = code;
        this.shape = shape;
    }

    public int code() {
        return code;
    }

    public GerberShape shape() {
        return shape;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        extended(writer, body -> body.word("AD").word("D").number(code).write(shape));
    }
}
