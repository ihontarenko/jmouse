package org.jmouse.template;

import org.jmouse.template.lexer.Token.Type;
import org.jmouse.template.lexer.TokenizableString;
import org.jmouse.util.Exceptions;
import org.jmouse.util.helper.Arrays;

import java.util.Iterator;

/**
 * Represents a tokenized string, allowing efficient access to tokenized segments.
 *
 * <p>Maintains a list of tokens along with their respective offsets and lengths within
 * the original character sequence.</p>
 *
 * <pre>{@code
 * Tokenized tokenizedString = new Tokenized("Hello, world!");
 * tokenizedString.entry(0, 5, TokenType.HELLO);
 * System.out.println(tokenizedString.get(0));
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class Tokenized implements TokenizableString {

    private static final int DEFAULT_ARRAY_SIZE = 32;

    private final CharSequence sequence;
    private       int          size = 0;
    private       Type[]       types;
    private       int[]        offsets;
    private       int[]        lengths;

    /**
     * Constructs a new {@code Tokenized} instance for the given character sequence.
     *
     * @param sequence the input character sequence to tokenize
     */
    public Tokenized(CharSequence sequence) {
        this.sequence = sequence;
        this.types = new Type[DEFAULT_ARRAY_SIZE];
        this.offsets = new int[DEFAULT_ARRAY_SIZE];
        this.lengths = new int[DEFAULT_ARRAY_SIZE];
    }

    /**
     * Adds a type entry with its offset and length.
     *
     * @param offset the starting offset of the type in the sequence
     * @param length the length of the type
     * @param token  the type type
     */
    @Override
    public void entry(int offset, int length, Type token) {
        ensureCapacity();

        this.offsets[this.size] = offset;
        this.lengths[this.size] = length;
        this.types[this.size] = token;

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
     * Returns the length of the original character sequence.
     *
     * @return the length of the sequence
     */
    @Override
    public int length() {
        return sequence.length();
    }

    /**
     * Returns the character at the specified index.
     *
     * @param index the character index
     * @return the character at the specified index
     */
    @Override
    public char charAt(int index) {
        return sequence.charAt(index);
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
        return sequence.subSequence(start, end);
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
            this.offsets = Arrays.expand(this.offsets, this.offsets.length << 1);
            this.types = Arrays.expand(this.types, this.types.length << 1);
            this.lengths = Arrays.expand(this.lengths, this.lengths.length << 1);
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
        return sequence.toString();
    }
}