package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

/** {@code M02*}: the end of the file; nothing after it is read. */
public final class EndOfFile extends GerberCommand {

    public EndOfFile(GerberPosition position) {
        super(position);
    }

    @Override
    public void writeSource(SourceWriter writer) {
        block(writer, words -> words.word("M02"));
    }
}
