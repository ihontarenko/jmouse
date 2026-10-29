package org.jmouse.gerber.node;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.gerber.GerberPosition;

import java.util.function.Consumer;

/**
 * One command of a Gerber file. Extended commands — between {@code %} — set the file up: its format, units,
 * apertures, attributes and polarity; word commands draw with them. A command the format has that no node is named for
 * is kept as written — {@link WordBlock}, {@link ExtendedBlock} — so nothing read is lost on its way back.
 */
public abstract sealed class GerberCommand extends GerberNode
        permits FormatSpecification, UnitMode, ApertureDefinition, Attribute, LoadPolarity, SelectAperture, Operation,
        InterpolationMode, QuadrantMode, Region, Comment, EndOfFile, WordBlock, ExtendedBlock {

    /** What opens and closes an extended command. */
    static final String EXTENDED = "%";
    /** What ends a block. */
    static final String END_OF_BLOCK = "*";

    protected GerberCommand(GerberPosition position) {
        super(position);
    }

    /** An extended command: its body between two {@code %}, its block ended. */
    static void extended(SourceWriter writer, Consumer<SourceWriter> body) {
        writer.word(EXTENDED);
        body.accept(writer);
        writer.join(END_OF_BLOCK).join(EXTENDED).endLine();
    }

    /** A word command: its words, then the end of its block. */
    static void block(SourceWriter writer, Consumer<SourceWriter> words) {
        words.accept(writer);
        writer.join(END_OF_BLOCK).endLine();
    }
}
