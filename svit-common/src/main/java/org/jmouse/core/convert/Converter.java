package org.jmouse.core.convert;

import org.jmouse.core.reflection.Reflections;

import java.util.Objects;

/**
 * A functional interface representing a simple conversion operation from a source
 * type {@code S} to a target type {@code T}. This interface is commonly used in
 * contexts where objects of one type need to be transformed into another type,
 * such as data mapping or serialization.
 *
 * @param <S> the source type
 * @param <T> the target type
 */
@FunctionalInterface
public interface Converter<S, T> {

    /**
     * Converts the specified source object of type {@code S} to an object of type {@code T}.
     *
     * @param source the source object to convert
     * @return the converted object
     */
    T convert(S source);

    default <U> Converter<S, U> andThen(Converter<? super T, ? extends U> after) {
        return (S value) -> {
            T initial = Objects.requireNonNull(convert(value), "Converter '%s' must return non NULL result"
                    .formatted(Reflections.getShortName(getClass())));
            return Objects.requireNonNull(after, "Next converter in chain can not be NULL").convert(initial);
        };
    }

}
