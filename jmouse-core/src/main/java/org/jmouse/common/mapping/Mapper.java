package org.jmouse.common.mapping;

import org.jmouse.core.reflection.Reflections;

/**
 * Interface for mapping between two types, typically a source (S) and a destination (D).
 * Provides methods for direct and reverse mapping, as well as optional in-place mapping.
 *
 * @param <S> the source type
 * @param <D> the destination type
 */
public interface Mapper<S, D> {

    /**
     * Maps an bean of type S to type D.
     *
     * @param source the source bean to map
     * @return the mapped bean of type D
     */
    D map(S source);

    /**
     * Maps an bean of type D back to type S (reverse mapping).
     *
     * @param source the source bean to map
     * @return the mapped bean of type S
     */
    S reverse(D source);

    /**
     * Maps an bean of type S to an existing instance of type D.
     * This method is optional and throws {@link UnsupportedOperationException} by default.
     *
     * @param source the source bean to map
     * @param destination the destination bean to populate
     * @throws UnsupportedOperationException if the method is not implemented
     */
    default void map(S source, D destination) {
        throw new UnsupportedOperationException();
    }

    /**
     * Maps an bean of type D to an existing instance of type S (reverse mapping).
     * This method is optional and throws {@link UnsupportedOperationException} by default.
     *
     * @param source the source bean to map
     * @param destination the destination bean to populate
     * @throws UnsupportedOperationException if the method is not implemented
     */
    default void reverse(D source, S destination) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the given bean is supported by this mapper.
     * This is determined by comparing the bean's class to the parameterized type of S.
     *
     * @param source the bean to check
     * @return {@code true} if the bean is supported, {@code false} otherwise
     */
    default boolean supports(Object source) {
        Class<?> preferredType = Reflections.getInterfacesParameterizedType(getClass(), Mapper.class, 0);

        if (preferredType != null) {
            return preferredType.equals(source.getClass());
        }

        return true;
    }
}
