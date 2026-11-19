package org.jmouse.java.parser.source;

/**
 * A range of the original source text: {@code start} inclusive, {@code end} exclusive.
 *
 * <p>Every span this module produces ends just after a character that is not a line terminator, so
 * {@code end().line()} is always the line of the last character the span covers.
 *
 * @param start the first position covered
 * @param end   the position just after the last character covered
 * @see SourcePosition for the coordinate system
 */
public record SourceSpan(SourcePosition start, SourcePosition end) {

    /**
     * Validates that the range is not reversed.
     */
    public SourceSpan {
        if (start == null || end == null || end.offset() < start.offset()) {
            throw new IllegalArgumentException("A source span needs a start and an end that does not precede it.");
        }
    }

    /**
     * Returns the number of UTF-16 code units covered.
     *
     * @return {@code end.offset - start.offset}
     */
    public int length() {
        return end.offset() - start.offset();
    }

    /**
     * Returns the one-based first line covered.
     *
     * @return the start line
     */
    public int startLine() {
        return start.line();
    }

    /**
     * Returns the one-based last line covered.
     *
     * @return the end line
     */
    public int endLine() {
        return end.line();
    }

    /**
     * Returns whether another span lies entirely inside this one.
     *
     * @param other the span to test
     * @return {@code true} when {@code other} starts and ends within this span
     */
    public boolean contains(SourceSpan other) {
        return other.start.offset() >= start.offset() && other.end.offset() <= end.offset();
    }

    /**
     * Cuts the covered text out of the original it was measured on.
     *
     * @param original the exact text the span was produced from
     * @return the covered characters
     */
    public String slice(CharSequence original) {
        return original.subSequence(start.offset(), end.offset()).toString();
    }

    @Override
    public String toString() {
        return start + "-" + end;
    }
}
