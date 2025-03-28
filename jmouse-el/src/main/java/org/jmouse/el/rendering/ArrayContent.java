package org.jmouse.el.rendering;

import java.util.Arrays;

import static org.jmouse.util.helper.Arrays.expand;

/**
 * 📋 ArrayContent is an implementation of the Content interface using a dynamically expandable character array.
 * <p>
 * It supports appending and prepending character data while ensuring adequate capacity.
 * </p>
 */
public final class ArrayContent implements Content {

    private static final int    DEFAULT_CAPACITY = 128;
    private              char[] array            = new char[DEFAULT_CAPACITY];
    private              int    length           = 0;

    /**
     * Appends the given character data to the end of the content.
     * <p>
     * Ensures there is sufficient capacity and then copies the new data into the internal buffer.
     * </p>
     *
     * @param data the character array to append
     */
    @Override
    public void append(char[] data) {
        int length = data.length;
        ensureCapacity(this.length + length);
        // Copy new data at the end of the existing content
        System.arraycopy(data, 0, array, this.length, length);
        this.length += length;
    }

    /**
     * Returns a copy of the current content as a character array.
     *
     * @return a character array containing the rendered content
     */
    @Override
    public char[] getDataArray() {
        return Arrays.copyOf(array, length);
    }

    /**
     * Returns the number of characters currently stored in the content.
     *
     * @return the length of the content
     */
    @Override
    public int length() {
        return length;
    }

    /**
     * Ensures that the internal buffer has enough capacity to hold the specified number of characters.
     *
     * @param capacity the required minimum capacity
     */
    private void ensureCapacity(int capacity) {
        if (capacity > array.length) {
            grow(capacity);
        }
    }

    /**
     * Expands the internal buffer to at least the specified capacity.
     *
     * @param capacity the required minimum capacity
     */
    private void grow(int capacity) {
        array = expand(array, Math.max(array.length << 1, capacity));
    }

    @Override
    public String toString() {
        return new String(getDataArray());
    }
}
