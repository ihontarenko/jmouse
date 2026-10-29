package org.jmouse.spice.node;

import org.jmouse.el.writer.SourceWritable;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.el.writer.TextSourceWriter;
import org.jmouse.spice.SpicePosition;

/**
 * An expression as SPICE writes one — between braces, or between single quotes as some SPICEs allow — kept as its
 * text. SPICE's expression dialects differ from one simulator to the next; reading one is the caller's, in its own
 * expression language.
 *
 * @param text     the text between the delimiters, as written
 * @param position where the opening delimiter stands
 */
public record SpiceExpression(String text, Delimiter delimiter, SpicePosition position) implements SourceWritable {

    /** How an expression was delimited, so that it is written back the same way. */
    public enum Delimiter {
        BRACES("{", "}"), QUOTES("'", "'");

        private final String opening;
        private final String closing;

        Delimiter(String opening, String closing) {
            this.opening = opening;
            this.closing = closing;
        }
    }

    /** The expression with its delimiters, as SPICE writes it. */
    @Override
    public void writeSource(SourceWriter writer) {
        writer.openingWord(delimiter.opening).join(text).join(delimiter.closing);
    }

    /** The expression as SPICE writes it, on its own. */
    public String written() {
        return TextSourceWriter.render(this, SpiceNode.LAYOUT);
    }

    /** An expression as the lexer hands it over, delimiters included. */
    public static SpiceExpression of(String withDelimiters, SpicePosition position) {
        Delimiter delimiter = withDelimiters.startsWith("{") ? Delimiter.BRACES : Delimiter.QUOTES;
        return new SpiceExpression(withDelimiters.substring(1, withDelimiters.length() - 1), delimiter, position);
    }
}
