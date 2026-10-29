package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

import java.util.List;

/** A whole Gerber file: its commands, in order, each on a line of its own when written. */
public final class GerberDocument extends GerberNode {

    public GerberDocument(GerberPosition position) {
        super(position);
    }

    /** A file a program builds: its commands. */
    public static GerberDocument of(List<? extends GerberCommand> commands) {
        GerberDocument document = new GerberDocument(GerberPosition.NONE);
        commands.forEach(document::add);
        return document;
    }

    /** The commands in order; a region holds its own. */
    public List<GerberCommand> commands() {
        return getChildren(GerberCommand.class);
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.each(commands());
    }
}
