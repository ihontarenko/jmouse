package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

import java.util.List;

/**
 * {@code G36*} … {@code G37*}: an area — the contours its operations draw, filled in the polarity in hand. Its
 * commands are its children: a move starts a contour, a draw adds an edge.
 */
public final class Region extends GerberCommand {

    public Region(GerberPosition position) {
        super(position);
    }

    /** A region a program builds: its commands. */
    public static Region of(List<? extends GerberCommand> commands) {
        Region region = new Region(GerberPosition.NONE);
        commands.forEach(region::add);
        return region;
    }

    public List<GerberCommand> commands() {
        return getChildren(GerberCommand.class);
    }

    @Override
    public void writeSource(SourceWriter writer) {
        block(writer, words -> words.word("G36"));
        writer.each(commands());
        block(writer, words -> words.word("G37"));
    }
}
