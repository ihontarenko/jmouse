package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

/** {@code D10*}: the aperture what follows is drawn with. */
public final class SelectAperture extends GerberCommand {

    private final int code;

    public SelectAperture(int code, GerberPosition position) {
        super(position);
        this.code = code;
    }

    public int code() {
        return code;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        block(writer, words -> words.word("D").number(code));
    }
}
