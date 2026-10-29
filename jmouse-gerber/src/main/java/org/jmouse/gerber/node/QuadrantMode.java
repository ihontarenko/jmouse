package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

/** {@code G75*} — an arc may run through every quadrant — or {@code G74*}, the single-quadrant mode older files use. */
public final class QuadrantMode extends GerberCommand {

    private final boolean multiQuadrant;

    public QuadrantMode(boolean multiQuadrant, GerberPosition position) {
        super(position);
        this.multiQuadrant = multiQuadrant;
    }

    public boolean multiQuadrant() {
        return multiQuadrant;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        block(writer, words -> words.word(multiQuadrant ? "G75" : "G74"));
    }
}
