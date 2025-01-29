package org.jmouse.util;

import java.util.HashSet;
import java.util.Set;

/**
 * A generic Visitor interface that defines operations for handling elements of type {@code T}.
 * This interface can be implemented by classes that need to traverse or process elements,
 * deciding how to handle familiar or unfamiliar elements, perform actions, and forget elements.
 *
 * @param <T> the type of elements to be visited
 */
public interface Visitor<T> {

    /**
     * Called when an element is recognized as familiar.
     *
     * @param element the element that is familiar
     * @return {@code true} if the element was successfully recognized and handled as familiar,
     *         otherwise {@code false}
     */
    boolean familiar(T element);

    /**
     * Called when an element is recognized as unfamiliar.
     *
     * @param element the element that is unfamiliar
     * @return {@code true} if the element was successfully recognized and handled as unfamiliar,
     *         otherwise {@code false}
     */
    boolean unknown(T element);

    /**
     * Performs the main visiting action on the provided element. This method defines the
     * core behavior of the visitor when encountering an element.
     *
     * @param element the element to visit
     */
    void visit(T element);

    /**
     * Called when the visitor should forget or remove references to the given element.
     *
     * @param element the element to forget
     * @return {@code true} if the element was successfully forgotten, otherwise {@code false}
     */
    boolean forget(T element);

    /**
     * Clears all visited elements, effectively erasing the visitor's memory.
     * After calling this method, the visitor will treat all elements as unfamiliar.
     */
    void erase();

    /**
     * Retrieves a set of all elements that have been visited so far by this visitor.
     *
     * @return a {@link Set} containing all visited elements of type {@code T}
     */
    Set<T> encounters();

    /**
     * Creates and returns a new default implementation of a {@link Visitor} for type {@code U}.
     * This default visitor uses a {@link HashSet} to keep track of visited elements.
     *
     * @param <U> the type of elements the visitor will handle
     * @return a new instance of {@link Visitor.Default} for type {@code U}
     */
    static <U> Visitor<U> createDefault() {
        return new Default<>();
    }

    /**
     * A default implementation of the {@link Visitor} interface.
     * It keeps track of visited elements using a {@link HashSet} and provides basic
     * implementations for recognizing familiar/unfamiliar elements, visiting, and forgetting elements.
     *
     * @param <T> the type of elements to be visited
     */
    class Default<T> implements Visitor<T> {

        private final Set<T> visited = new HashSet<>();

        /**
         * Checks if the given element has been visited before.
         *
         * @param element the element to check
         * @return {@code true} if the element is familiar (has been visited), {@code false} otherwise
         */
        @Override
        public boolean familiar(T element) {
            return visited.contains(element);
        }

        /**
         * Checks if the given element has not been visited before.
         *
         * @param element the element to check
         * @return {@code true} if the element is unfamiliar (has not been visited), {@code false} otherwise
         */
        @Override
        public boolean unknown(T element) {
            return !visited.contains(element);
        }

        /**
         * Marks the given element as visited.
         *
         * @param element the element to visit
         */
        @Override
        public void visit(T element) {
            visited.add(element);
        }

        /**
         * Forgets the given element, removing it from the set of visited elements.
         *
         * @param element the element to forget
         * @return {@code true} if the element was successfully removed, {@code false} otherwise
         */
        @Override
        public boolean forget(T element) {
            return visited.remove(element);
        }

        /**
         * Erases all remembered elements, clearing the visitor's memory.
         */
        @Override
        public void erase() {
            visited.clear();
        }

        /**
         * All visited elements
         */
        @Override
        public Set<T> encounters() {
            return Set.copyOf(visited);
        }
    }

}
