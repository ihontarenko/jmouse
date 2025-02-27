package org.jmouse.template;

import org.jmouse.template.lexer.Tokenizable;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

import static org.jmouse.util.helper.Arrays.expand;

/**
 * Represents a template source that reads data from a {@link Reader} and provides
 * efficient operations for text manipulation.
 *
 * <p>This class maintains a buffer to store template content, supporting operations
 * like substring extraction, buffer expansion, and line tracking.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class TemplateSource implements CharSequence {

    private static final int DEFAULT_CAPACITY = 1024;

    private final String      template;          // Template name
    private final Tokenizable text;              // Tokenized representation of the template
    private       int         lineNumber = 1;    // Tracks current line number
    private       int         offset     = 0;    // Offset of the buffer start
    private       int         size       = 0;    // Number of characters stored in buffer
    private       char[]      buffer;            // Character buffer for storing template data

    /**
     * Constructs a {@code TemplateSource} with the given template name and content reader.
     *
     * @param template the name of the template
     * @param reader   the reader providing the template content
     */
    public TemplateSource(String template, Reader reader) {
        this.template = template;
        this.buffer = new char[DEFAULT_CAPACITY];
        copyReaderIntoCharArray(reader);
        this.text = new TokenizedString(this);
    }

    /**
     * Appends characters from a buffer into the internal buffer.
     *
     * @param buffer the character buffer to append
     * @param length the number of characters to append
     */
    public void append(char[] buffer, int length) {
        System.arraycopy(buffer, 0, this.buffer, size, length);
        size += length;
    }

    /**
     * Returns the current offset of the buffer.
     *
     * @return the current offset
     */
    public int offset() {
        return offset;
    }

    /**
     * Returns the current size of the stored content in the buffer.
     *
     * @return the buffer size
     */
    public int size() {
        return size;
    }

    /**
     * Returns the tokenized representation of the template.
     *
     * @return the tokenized content
     */
    public Tokenizable text() {
        return text;
    }

    /**
     * Shifts the buffer forward by the specified length, adjusting the offset.
     * Tracks new lines encountered in the shifted content.
     *
     * @param length the number of characters to shift
     */
    public void shift(int length) {
        this.offset += length;
        this.size -= length;
        ensureBounds();
        calculateLineNumber(this.offset);
    }

    /**
     * Resets the buffer offset and size, clearing stored content.
     */
    public void reset() {
        this.offset = 0;
        this.size = 0;
    }

    /**
     * Retracts the buffer by the specified length, moving the offset backwards.
     * The available size is increased accordingly, and line numbers are recalculated.
     *
     * @param length the number of characters to move backwards
     */
    public void retract(int length) {
        this.offset -= length;
        this.size += length;
        ensureBounds();
        calculateLineNumber(this.offset);
    }

    /**
     * Ensures the offset and size are within valid bounds.
     */
    private void ensureBounds() {
        this.offset = Math.min(Math.max(0, this.offset), buffer.length);
        this.size = Math.min(Math.max(0, this.size), buffer.length);
    }

    /**
     * Extracts a substring from the buffer between specified positions.
     *
     * @param start the starting offset (relative to offset)
     * @param end   the ending offset (relative to offset)
     * @return the extracted substring
     */
    public String substring(int start, int end) {
        return new String(Arrays.copyOfRange(buffer, offset + start, offset + end));
    }

    /**
     * Reads the entire content of the given {@link Reader} into the internal buffer.
     *
     * @param reader the reader providing template content
     */
    private void copyReaderIntoCharArray(Reader reader) {
        char[] buffer = new char[DEFAULT_CAPACITY * 4];
        int    length;

        try {
            while ((length = reader.read(buffer)) != -1) {
                ensureCapacity(size + length);
                append(buffer, length);
            }
        } catch (IOException exception) {
            throw new TemplateException("Error reading template", exception);
        }
    }

    /**
     * Ensures that the internal buffer has enough capacity, expanding if necessary.
     *
     * @param capacity the required minimum capacity
     */
    private void ensureCapacity(int capacity) {
        if (0 > buffer.length - capacity) {
            grow(capacity);
        }
    }

    /**
     * Expands the internal buffer to accommodate additional characters.
     *
     * @param capacity the required minimum capacity
     */
    private void grow(int capacity) {
        buffer = expand(buffer, Math.max(buffer.length << 1, capacity));
    }

    /**
     * Determines the length of a newline sequence at the specified offset.
     * Supports different newline formats: '\n', '\r', and '\r\n'.
     *
     * @param offset the offset to check for a newline
     */
    private void calculateLineNumber(int offset) {
        int lines = 0;

        for (int i = 0; i < offset; i++) {
            char character = charAt(i);
            if (character == '\r' && charAt(offset + 1) == '\n') {
                lines++;
                i++;
            } else if (character == '\n' || character == '\r') {
                lines++;
            }
        }

        this.lineNumber = lines;
    }

    /**
     * Returns the current line number in the template.
     *
     * @return the current line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the name of the template.
     *
     * @return the template name
     */
    public String getTemplateName() {
        return template;
    }

    /**
     * Returns the total length of the available characters in the buffer.
     *
     * @return the buffer length
     */
    @Override
    public int length() {
        return size;
    }

    /**
     * Returns the character at the specified index.
     *
     * @param index the character index
     * @return the character at the specified index
     */
    @Override
    public char charAt(int index) {
        return buffer[offset + index];
    }

    /**
     * Returns a subsequence of the buffer between specified positions.
     *
     * @param start the start index
     * @param end   the end index
     * @return a subsequence of the buffer
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }

    /**
     * Returns the template content as a string.
     *
     * @return the template content as a string
     */
    @Override
    public String toString() {
        return new String(Arrays.copyOfRange(buffer, offset, size + offset));
    }
}