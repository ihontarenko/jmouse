package org.jmouse.el.extension;

import org.jmouse.util.Streamable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a collection of arguments passed to an expression or function.
 * <p>
 * This interface extends {@link org.jmouse.util.Streamable} to allow iteration over the arguments.
 * It provides methods for retrieving individual arguments by index, obtaining the first or last argument,
 * checking the size of the collection, and verifying if the collection is empty.
 * </p>
 */
public interface Arguments extends Streamable<Object> {

    /**
     * Creates an {@code Arguments} instance from the provided array of objects.
     *
     * @param arguments an array of objects representing the arguments
     * @return an {@code Arguments} instance wrapping the given arguments
     */
    static Arguments forArray(Object... arguments) {
        return new Basic(arguments);
    }

    /**
     * Returns an empty {@code Arguments} instance.
     *
     * @return an empty {@code Arguments} instance
     */
    static Arguments empty() {
        return new Basic();
    }

    /**
     * Retrieves the argument at the specified index.
     *
     * @param index the index of the argument to retrieve
     * @return the argument at the given index, or {@code null} if the index is out of bounds
     */
    Object get(int index);

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
     * A basic implementation of the {@code Arguments} interface that wraps a list of objects.
     */
    class Basic implements Arguments {

        private final List<Object> arguments;

        /**
         * Constructs a new {@code Basic} Arguments instance using the provided list.
         *
         * @param arguments a list of objects representing the arguments
         */
        public Basic(List<Object> arguments) {
            this.arguments = arguments;
        }

        /**
         * Constructs a new {@code Basic} Arguments instance using the provided array of objects.
         *
         * @param arguments an array of objects representing the arguments
         */
        public Basic(Object... arguments) {
            this.arguments = Arrays.asList(arguments);
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

            if (index < arguments.size()) {
                value = arguments.get(index);
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
            return arguments.size();
        }

        /**
         * Returns an iterator over the arguments.
         *
         * @return an {@link Iterator} over the arguments
         */
        @Override
        public Iterator<Object> iterator() {
            return arguments.iterator();
        }
    }
}
