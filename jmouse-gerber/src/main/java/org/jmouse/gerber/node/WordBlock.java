package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

import java.util.List;

/** A word command no node is named for — {@code G54D10*}, {@code M00*} in an old file —, kept as its words. */
public final class WordBlock extends GerberCommand {

    private final List<String> words;

    public WordBlock(List<String> words, GerberPosition position) {
        super(position);
        this.words = List.copyOf(words);
    }

    public List<String> words() {
        return words;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        block(writer, written -> words.forEach(written::word));
    }
}
