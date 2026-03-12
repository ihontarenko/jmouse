package org.jmouse.core;

/**
 * Supplier that accepts a single argument. 🧩
 *
 * <p>
 * Functional variant of {@code Supplier} where value creation
 * depends on an input parameter.
 * </p>
 *
 * @param <A> argument type
 * @param <T> supplied value type
 */
@FunctionalInterface
public interface ParameterizedSupplier<A, T> {

    /**
     * Returns a value using the given argument.
     *
     * @param argument input argument
     *
     * @return supplied value
     */
    T get(A argument);
}