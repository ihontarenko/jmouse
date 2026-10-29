package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

/** {@code G01*}, {@code G02*}, {@code G03*}: how the draws that follow run. */
public final class InterpolationMode extends GerberCommand {

    private final Interpolation interpolation;

    public InterpolationMode(Interpolation interpolation, GerberPosition position) {
        super(position);
        this.interpolation = interpolation;
    }

    public Interpolation interpolation() {
        return interpolation;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        block(writer, words -> words.word(interpolation.code()));
    }
}
