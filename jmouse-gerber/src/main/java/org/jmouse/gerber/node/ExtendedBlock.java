package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

import java.util.List;

/**
 * An extended command no node is named for — a macro ({@code %AM…*%}), a step and repeat ({@code %SR…*%}), an old
 * image command —, kept as its blocks.
 */
public final class ExtendedBlock extends GerberCommand {

    private final List<String> blocks;

    public ExtendedBlock(List<String> blocks, GerberPosition position) {
        super(position);
        this.blocks = List.copyOf(blocks);
    }

    public List<String> blocks() {
        return blocks;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        extended(writer, body -> {
            for (int index = 0; index < blocks.size(); index++) {
                if (index > 0) {
                    body.join(END_OF_BLOCK);
                }
                body.word(blocks.get(index));
            }
        });
    }
}
