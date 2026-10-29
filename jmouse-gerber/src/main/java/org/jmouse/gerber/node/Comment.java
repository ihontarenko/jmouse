package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

/** {@code G04 what it says*}: a comment. */
public final class Comment extends GerberCommand {

    private final String text;

    public Comment(String text, GerberPosition position) {
        super(position);
        this.text = text.replaceAll("[*%\\r\\n]", " ").strip();
    }

    public String text() {
        return text;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        // A comment's text is the one place Gerber keeps a blank: it is the text, not a separator between words.
        block(writer, words -> words.word("G04").join(text.isEmpty() ? "" : " ").join(text));
    }
}
