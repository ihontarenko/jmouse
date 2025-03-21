package org.jmouse.util;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.core.reflection.TypeInformation;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public interface Values extends Streamable<Object> {

    /**
     * Creates an {@code Values} instance from the provided array of objects.
     *
     * @param values an array of objects representing the values
     * @return an {@code Values} instance wrapping the given values
     */
    static Values forArray(Object... values) {
        return new Basic(values);
    }

    /**
     * Returns an empty {@code Values} instance.
     *
     * @return an empty {@code Values} instance
     */
    static Values empty() {
        return new Basic();
    }

    /**
     * Retrieves the argument at the specified index.
     *
     * @param index the index of the argument to retrieve
     * @return the argument at the given index, or {@code null} if the index is out of bounds
     */
    Object get(int index);

    default ClassTypeInspector getType(int index) {
        return TypeInformation.forInstance(get(index));
    }

    /**
     * Retrieves the first argument in the collection.
     *
     * @return the first argument, or {@code null} if there are no arguments
     */
    default Object getFirst() {
        return get(0);
    }

    /**
     * Retrieves the last argument in the collection.
     *
     * @return the last argument, or {@code null} if there are no arguments
     */
    default Object getLast() {
        return get(size() - 1);
    }

    /**
     * Returns the total number of arguments in the collection.
     *
     * @return the number of arguments
     */
    int size();

    /**
     * Checks whether the collection of arguments is empty.
     *
     * @return {@code true} if there are no arguments; {@code false} otherwise
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Checks whether the collection of arguments is not empty.
     *
     * @return {@code true} if there is at least one argument; {@code false} otherwise
     */
    default boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * A basic implementation of the {@code Values} interface that wraps a list of objects.
     */
    class Basic implements Values {

        private final List<Object> values;

        /**
         * Constructs a new {@code Basic} Values instance using the provided array of objects.
         *
         * @param values an array of objects representing the arguments
         */
        public Basic(Object... values) {
            this.values = Arrays.asList(values);
        }

        /**
         * Retrieves the argument at the specified index.
         *
         * @param index the index of the argument to retrieve
         * @return the argument at the given index, or {@code null} if the index is out of bounds
         */
        @Override
        public Object get(int index) {
            Object value = null;

            if (index < values.size()) {
                value = values.get(index);
            }

            return value;
        }

        /**
         * Returns the total number of arguments in this collection.
         *
         * @return the number of arguments
         */
        @Override
        public int size() {
            return values.size();
        }

        /**
         * Returns an iterator over the arguments.
         *
         * @return an {@link Iterator} over the arguments
         */
        @Override
        public Iterator<Object> iterator() {
            return values.iterator();
        }

        @Override
        public String toString() {
            return Arrays.toString(values.toArray());
        }

    }

}
