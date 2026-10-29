package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

/** {@code %MOMM*%} or {@code %MOIN*%}: the unit every length of the file is in. */
public final class UnitMode extends GerberCommand {

    private final boolean millimetres;

    public UnitMode(boolean millimetres, GerberPosition position) {
        super(position);
        this.millimetres = millimetres;
    }

    public boolean millimetres() {
        return millimetres;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        extended(writer, body -> body.word("MO").word(millimetres ? "MM" : "IN"));
    }
}
