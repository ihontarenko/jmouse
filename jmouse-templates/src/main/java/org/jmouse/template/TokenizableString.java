package org.jmouse.template;

import org.jmouse.template.lexer.Token.Type;
import org.jmouse.template.lexer.TokenizableSource;
import org.jmouse.util.Exceptions;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;

import static org.jmouse.util.helper.Arrays.expand;

/**
 * Represents a tokenized string, allowing efficient access to tokenized segments.
 *
 * <p>Maintains a list of tokens along with their respective offsets and lengths within
 * the original character sequence.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class TokenizableString implements TokenizableSource {

    private static final int DEFAULT_ARRAY_SIZE = 32;
    private static final int DEFAULT_CAPACITY   = 1024;

    private final String name;          // Template name
    private       int    length = 0;    // Number of characters stored in buffer
    private       char[] buffer;        // Character buffer for storing template data
    private       int    size   = 0;
    private       Type[] types;
    private       int[]  offsets;
    private       int[]  lengths;

    public TokenizableString(String name, String text) {
        this(name, new StringReader(text));
    }

    public TokenizableString(String name, Reader reader) {
        this.name = name;
        this.types = new Type[DEFAULT_ARRAY_SIZE];
        this.offsets = new int[DEFAULT_ARRAY_SIZE];
        this.lengths = new int[DEFAULT_ARRAY_SIZE];
        this.buffer = new char[DEFAULT_CAPACITY];

        toCharArray(reader);
    }

    /**
     * Reads the entire content of the given {@link Reader} into the internal buffer.
     *
     * @param reader the reader providing template content
     */
    private void toCharArray(Reader reader) {
        char[] buffer = new char[DEFAULT_CAPACITY * 4];
        int    length;

        try {
            while ((length = reader.read(buffer)) != -1) {
                ensureCapacity(this.length + length);
                System.arraycopy(buffer, 0, this.buffer, this.length, length);
                this.length += length;
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
     * Calculates the number of lines up to a given offset.
     *
     * @param offset the offset to check for newlines
     * @return the total number of lines up to the given offset
     */
    @Override
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
    @Override
    public String getName() {
        return name;
    }

    /**
     * Adds a type entry with its offset and length.
     *
     * @param offset the starting offset of the type in the sequence
     * @param length the length of the type
     * @param type  the token type
     */
    @Override
    public void entry(int offset, int length, Type type) {
        ensureCapacity();

        this.offsets[this.size] = offset;
        this.lengths[this.size] = length;
        this.types[this.size] = type;

        this.size++;
    }

    /**
     * Retrieves the type entry at the specified index.
     *
     * @param index the index of the type
     * @return the corresponding {@link Entry}
     */
    @Override
    public Entry get(int index) {
        ensureIndex(index);

        int  offset = this.offsets[index];
        Type token  = this.types[index];
        int  length = this.lengths[index];

        String segment = subSequence(offset, offset + length).toString();

        return new Entry(offset, length, token, segment);
    }

    /**
     * Returns the number of stored type entries.
     *
     * @return the number of type entries
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Extracts a substring from the buffer between specified positions.
     *
     * @param start the starting offset
     * @param end   the ending offset
     * @return the extracted substring
     */
    public String substring(int start, int end) {
        return new String(Arrays.copyOfRange(buffer, start, end));
    }

    /**
     * Returns the length of the original character sequence.
     *
     * @return the length of the sequence
     */
    @Override
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
     * Returns a subsequence of the original character sequence.
     *
     * @param start the start index (inclusive)
     * @param end   the end index (exclusive)
     * @return the extracted subsequence
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }

    /**
     * Ensures that the index is within a valid range.
     *
     * @param index the index to validate
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    private void ensureIndex(int index) {
        if (index < 0 || index >= size()) {
            Exceptions.throwIfOutOfRange(index, 0, size, "Index: %d, Size: %d".formatted(index, size));
        }
    }

    /**
     * Ensures sufficient capacity in the arrays, expanding them if needed.
     */
    private void ensureCapacity() {
        if (this.offsets.length <= this.size) {
            this.offsets = expand(this.offsets, this.offsets.length << 1);
            this.types = expand(this.types, this.types.length << 1);
            this.lengths = expand(this.lengths, this.lengths.length << 1);
        }
    }

    /**
     * Returns an iterator over the type entries.
     *
     * @return an iterator for {@link Entry} elements
     */
    @Override
    public Iterator<Entry> iterator() {
        return new Iterator<>() {

            private int position = 0;

            @Override
            public boolean hasNext() {
                return position < size();
            }

            @Override
            public Entry next() {
                return get(position++);
            }

        };
    }

    @Override
    public String toString() {
        return new String(buffer, 0, length);
    }
}