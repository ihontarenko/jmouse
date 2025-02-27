package org.jmouse.template;

import org.jmouse.template.lexer.TokenizableString;

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
public class StringSource implements CharSequence {

    private static final int DEFAULT_CAPACITY = 1024;

    private final String            name;          // Template name
    private final TokenizableString tokenizable;
    private       int               length = 0;    // Number of characters stored in buffer
    private       char[]            buffer;        // Character buffer for storing template data

    /**
     * Constructs a {@code StringSource} with the given template name and content reader.
     *
     * @param name the name of the template
     * @param reader   the reader providing the template content
     */
    public StringSource(String name, Reader reader) {
        this.tokenizable = new Tokenized(this);
        this.name = name;
        this.buffer = new char[DEFAULT_CAPACITY];
        copyReaderIntoCharArray(reader);
    }

    public TokenizableString getTokenizable() {
        return tokenizable;
    }

    /**
     * Appends characters from a buffer into the internal buffer.
     *
     * @param buffer the character buffer to append
     * @param length the number of characters to append
     */
    public void append(char[] buffer, int length) {
        System.arraycopy(buffer, 0, this.buffer, this.length, length);
        this.length += length;
    }

    /**
     * Extracts a substring from the buffer between specified positions.
     *
     * @param start the starting offset (relative to offset)
     * @param end   the ending offset (relative to offset)
     * @return the extracted substring
     */
    public String substring(int start, int end) {
        return new String(Arrays.copyOfRange(buffer, start, end));
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
                ensureCapacity(this.length + length);
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
    public int getLineNumber(int offset) {
        int lines = 1;

        for (int i = 0; i < offset; i++) {
            char character = buffer[i];
            if (character == '\r' && buffer[i + 1] == '\n') {
                lines++;
                i++;
            } else if (character == '\n' || character == '\r') {
                lines++;
            }
        }

        return lines;
    }

    /**
     * Returns the name of the template.
     *
     * @return the template name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the total length of the available characters in the buffer.
     *
     * @return the buffer length
     */
    public int length() {
        return length;
    }

    /**
     * Returns the character at the specified index.
     *
     * @param index the character index
     * @return the character at the specified index
     */
    @Override
    public char charAt(int index) {
        return buffer[index];
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
        return new String(buffer, 0, length);
    }

}