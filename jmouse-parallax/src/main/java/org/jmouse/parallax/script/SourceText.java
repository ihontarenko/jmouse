package org.jmouse.parallax.script;

/**
 * Text exactly as the author wrote it, and where: an expression, an equation's right side, a range bound.
 *
 * <p>The reader never rebuilds an expression from tokens. It cuts the source between the tokens that bound it, so
 * {@code -0.8 + 0.156 i}, {@code 2 pi} and {@code exp(-(x - mu)^2)} reach jMF character for character. A compiler
 * that refuses the text adds its own offset to {@link #offset()} and reports the author's line and column.</p>
 *
 * @param text   the text, trimmed
 * @param line   the 1-based line it starts on
 * @param column the 1-based column it starts at
 * @param offset its position in the normalized source
 */
public record SourceText(String text, int line, int column, int offset) {

    public SourceText {
        text = text == null ? "" : text.strip();
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    @Override
    public String toString() {
        return text;
    }
}
