package org.jmouse.java.parser.source;

import java.util.Arrays;

/**
 * Turns offsets of one original text into {@link SourcePosition lines and columns}.
 *
 * <p>Built once per text in linear time; each lookup is a binary search over line starts.
 */
public final class LineIndex {

    private final int   length;
    private final int[] lineStarts;

    private LineIndex(int length, int[] lineStarts) {
        this.length = length;
        this.lineStarts = lineStarts;
    }

    /**
     * Indexes the line starts of a text.
     *
     * @param text the original text
     * @return the index
     */
    public static LineIndex of(CharSequence text) {
        int[] starts = new int[16];
        int   count  = 0;
        starts[count++] = 0;
        int offset = 0;
        int length = text.length();

        while (offset < length) {
            char character = text.charAt(offset);
            offset++;
            if (character == '\r' && offset < length && text.charAt(offset) == '\n') {
                offset++;
            }
            if (character == '\n' || character == '\r') {
                if (count == starts.length) {
                    starts = Arrays.copyOf(starts, count * 2);
                }
                starts[count++] = offset;
            }
        }

        return new LineIndex(length, Arrays.copyOf(starts, count));
    }

    /**
     * Returns the position of an offset.
     *
     * @param offset zero-based UTF-16 offset, from {@code 0} to the text length inclusive
     * @return its line and column
     */
    public SourcePosition position(int offset) {
        if (offset < 0 || offset > length) {
            throw new IllegalArgumentException("Offset " + offset + " lies outside the text.");
        }
        int lineIndex = lineIndexOf(offset);
        return new SourcePosition(offset, lineIndex + 1, offset - lineStarts[lineIndex] + 1);
    }

    /**
     * Returns the span between two offsets.
     *
     * @param startOffset inclusive start
     * @param endOffset   exclusive end
     * @return the span
     */
    public SourceSpan span(int startOffset, int endOffset) {
        return new SourceSpan(position(startOffset), position(endOffset));
    }

    /**
     * Returns the number of lines; an empty text has one empty line.
     *
     * @return the line count
     */
    public int lineCount() {
        return lineStarts.length;
    }

    private int lineIndexOf(int offset) {
        int low  = 0;
        int high = lineStarts.length - 1;

        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (lineStarts[middle] <= offset) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }

        return low;
    }
}
