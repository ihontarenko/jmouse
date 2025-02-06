package org.jmouse.beans;

import java.util.function.Supplier;

/**
 * Interface for detecting cyclic dependencies in bean creation or dependency graphs.
 * <p>
 * This interface provides methods to detect cycles and remove processed identifiers
 * to ensure that no cyclic references exist during runtime.
 *
 * @param <T> the type of identifier used to represent elements in the dependency graph.
 */
public interface CyclicReferenceDetector<T> {

    /**
     * Detects a potential cyclic reference in the dependency graph.
     * <p>
     * If a cyclic reference is detected, the provided {@link Supplier} is used to throw
     * a custom exception that describes the issue.
     *
     * @param identifier        the identifier representing the current element being processed.
     * @param exceptionSupplier a supplier that provides a custom exception to throw if a cycle is detected.
     * @throws Throwable the exception provided by the supplier if a cycle is detected.
     */
    void detect(Identifier<T> identifier, Supplier<? extends RuntimeException> exceptionSupplier);

    /**
     * Removes the specified identifier from the tracking structure.
     * <p>
     * This method should be called after processing an element to ensure the tracking
     * of cyclic references remains accurate.
     *
     * @param identifier the identifier to remove from the tracking structure.
     */
    void remove(Identifier<T> identifier);

    /**
     * Functional interface representing an identifier used to track elements in the dependency graph.
     *
     * @param <T> the type of the identifier.
     */
    @FunctionalInterface
    interface Identifier<T> {

        /**
         * Retrieves the unique identifier for the current element.
         *
         * @return the identifier of the element.
         */
        T getIdentifier();
    }
}
