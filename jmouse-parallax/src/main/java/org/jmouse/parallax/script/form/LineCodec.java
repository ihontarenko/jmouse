package org.jmouse.parallax.script.form;

import org.jmouse.parallax.script.Kind;

import java.util.List;
import java.util.Map;

/**
 * How one line of a block is edited as fields: read by the jMX reader itself, written back as one line.
 *
 * <p>A codec never parses: it hands the line to {@link org.jmouse.parallax.script.ExperienceReader} inside a one-line
 * document and takes the values from what the reader returns, so a line the reader refuses is refused here with the
 * reader's words. Writing is the one direction the reader does not cover, and the document a written line lands in is
 * read again before the form shows it.</p>
 */
interface LineCodec {

    /** The fields a line of this block may have, for the schema. */
    List<FieldSchema> fields();

    /**
     * A line's values, field by field, in the order a form shows them.
     *
     * @throws org.jmouse.parallax.script.ExperienceParseException when the reader refuses the line
     */
    List<FormField> read(Kind kind, String line);

    /**
     * The line the values describe.
     *
     * @param indent the whitespace the line starts with, for a value that continues on further lines
     */
    String write(Map<String, String> values, String indent);
}
