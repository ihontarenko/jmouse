package org.jmouse.parallax.script.form;

import java.util.List;

/**
 * One block of the language as a form edits it.
 *
 * @param name       the block's word
 * @param editor     how a form edits it
 * @param repeatable whether a document may hold it more than once
 * @param header     the fields written between the block's word and its brace: {@code symbol uses resistor-symbol}
 * @param line       the fields of each line inside its braces; one {@link FieldType#CODE} field when a line is kept as
 *                   written
 * @param kinds      the kinds of document that may hold it, as their keywords
 */
public record BlockSchema(String name, Editor editor, boolean repeatable, List<FieldSchema> header,
                          List<FieldSchema> line, List<String> kinds) {

    public BlockSchema {
        header = List.copyOf(header);
        line = List.copyOf(line);
        kinds = List.copyOf(kinds);
    }

    /** How a block is edited when a form shows it. */
    public enum Editor {
        /** Line by line, each line through its fields. */
        LINES,
        /** The body between the braces as one text in another language: jMF definitions, a jMS program. */
        BODY,
        /** On the schematic canvas: the parts and the wires between them. */
        SCHEMATIC,
        /** On the drawing canvas: a symbol's size, paths, pins and labels. */
        DRAWING,
        /** A statement of one line, through its fields. */
        STATEMENT
    }
}
