package org.jmouse.template.lexer;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Provides an immutable implementation of a {@link ListIterator}, preventing modifications
 * while allowing bidirectional iteration over a list of elements.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * List<String> items = List.of("a", "b", "c");
 * ImmutableListIterator<String> iterator = new SimpleLexer<>(items);
 * while (iterator.hasNext()) {
 *     System.out.println(iterator.next());
 * }
 * }</pre>
 *
 * @param <T> the type of elements in the list
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class ImmutableListIterator<T> implements ListIterator<T>, Iterable<T> {

    private final static UnsupportedOperationException UNSUPPORTED_OPERATION_EXCEPTION = new UnsupportedOperationException(
            "LIST ITERATOR IS IMMUTABLE");

    protected final List<T> entries;
    protected final int     length;
    protected       int     cursor;

    /**
     * Constructs an immutable list iterator over the given list.
     *
     * @param entries the list to iterate over
     */
    public ImmutableListIterator(List<T> entries) {
        this.length = entries.size();
        this.entries = Collections.unmodifiableList(entries);
    }

    /**
     * Moves the cursor to the previous element and returns it.
     *
     * @return the previous element in the list
     */
    @Override
    public T previous() {
        return entries.get(--cursor);
    }

    /**
     * Moves the cursor to the next element and returns it.
     *
     * @return the next element in the list
     */
    @Override
    public T next() {
        return entries.get(cursor++);
    }

    /**
     * Checks if there is a next element available.
     *
     * @return {@code true} if a next element exists, otherwise {@code false}
     */
    @Override
    public boolean hasNext() {
        return cursor != entries.size();
    }

    /**
     * Checks if there is a previous element available.
     *
     * @return {@code true} if a previous element exists, otherwise {@code false}
     */
    @Override
    public boolean hasPrevious() {
        return cursor != 0;
    }

    /**
     * Returns the index of the next element.
     *
     * @return the next element index
     */
    @Override
    public int nextIndex() {
        return cursor + 1;
    }

    /**
     * Returns the index of the previous element.
     *
     * @return the previous element index
     */
    @Override
    public int previousIndex() {
        return cursor - 1;
    }

    /**
     * Returns an iterator for the list.
     *
     * @return the iterator instance
     */
    @Override
    public Iterator<T> iterator() {
        return this;
    }

    /**
     * Unsupported operation - removal is not allowed.
     *
     * @throws UnsupportedOperationException always thrown
     */
    @Override
    public void remove() {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    /**
     * Unsupported operation - setting an element is not allowed.
     *
     * @throws UnsupportedOperationException always thrown
     */
    @Override
    public void set(T entry) {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    /**
     * Unsupported operation - adding an element is not allowed.
     *
     * @throws UnsupportedOperationException always thrown
     */
    @Override
    public void add(T entry) {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    /**
     * Returns a string representation of the iterator state.
     *
     * @return a formatted string showing the length and cursor position
     */
    @Override
    public String toString() {
        return String.format("ImmutableListIterator{length=%d, cursor=%d}", length, cursor);
    }
}
