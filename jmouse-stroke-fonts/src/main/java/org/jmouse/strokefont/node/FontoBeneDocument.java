package org.jmouse.strokefont.node;

import org.jmouse.el.writer.SourceWriter;

import java.util.List;

/** A FontoBene file: its lines in order — the header, the separator, then glyph after glyph. */
public final class FontoBeneDocument extends FontoBeneNode {

    public FontoBeneDocument() {
        super(1);
    }

    public List<FontoBeneLine> lines() {
        return getChildren(FontoBeneLine.class);
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.each(lines());
    }
}
