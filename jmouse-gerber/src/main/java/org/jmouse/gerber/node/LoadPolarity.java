package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

/** {@code %LPD*%} or {@code %LPC*%}: what follows is drawn dark — material — or clear, taken from what is under it. */
public final class LoadPolarity extends GerberCommand {

    private final boolean dark;

    public LoadPolarity(boolean dark, GerberPosition position) {
        super(position);
        this.dark = dark;
    }

    public boolean dark() {
        return dark;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        extended(writer, body -> body.word("LP").word(dark ? "D" : "C"));
    }
}
