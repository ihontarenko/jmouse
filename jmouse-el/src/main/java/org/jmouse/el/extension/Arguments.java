package org.jmouse.el.extension;

import org.jmouse.util.Values;

/**
 * Represents a collection of arguments for an expression or function.
 * <p>
 * This interface extends {@link org.jmouse.util.Values} to provide an iterable container of argument values.
 * It offers utility methods to access arguments by index, retrieve the first or last argument,
 * and check whether the collection is empty.
 * </p>
 */
public interface Arguments extends Values {

    /**
     * Creates an {@code Arguments} instance from the provided array of objects.
     *
     * @param arguments an array of objects representing the arguments
     * @return an {@code Arguments} instance encapsulating the given arguments
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
     * A basic implementation of the {@code Arguments} interface.
     * <p>
     * This implementation wraps an array of objects as arguments.
     * </p>
     */
    class Basic extends Values.Basic implements Arguments {
        /**
         * Constructs a new {@code Basic} Arguments instance with the specified values.
         *
         * @param values the objects to encapsulate as arguments
         */
        public Basic(Object... values) {
            super(values);
        }

    }
}
