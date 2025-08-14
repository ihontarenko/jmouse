package org.jmouse.util;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.core.reflection.TypeInformation;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;

/**
 * A utility class that provides helper methods for working with iterables.
 * <p>
 * This class contains static methods and is not meant to be instantiated.
 * </p>
 */
public class Iterables {

    /**
     * Private constructor to prevent instantiation.
     */
    private Iterables() {
        // Prevent instantiation of this utility class.
    }

    public static Iterable<?> toIterable(final Object object) {
        Iterable<?>        iterable = null;
        ClassTypeInspector type     = TypeInformation.forInstance(object);

        if (type.isIterable()) {
            iterable = ((Iterable<?>) object);
        } else if (type.is(Iterator.class)) {
            iterable = () -> (Iterator<Object>) object;
        } else if (type.isMap()) {
            iterable = ((Map<?, ?>) object).entrySet();
        } else if (type.isString()) {
            iterable = ((String) object).chars()
                    .mapToObj(character -> valueOf((char) character)).toList();
        } else if (type.isArray()) {
            iterable = List.of(((Object[]) object));
        }

        return iterable;
    }

    /**
     * Computes the size (number of elements) of the given {@link Iterable}.
     * <p>
     * This method iterates over all elements in the iterable and counts them.
     * It may be inefficient for large or infinite iterables.
     * </p>
     *
     * @param iterable the {@link Iterable} whose size is to be determined
     * @return the number of elements in the iterable
     * @throws NullPointerException if the provided iterable is {@code null}
     */
    public static int size(Iterable<?> iterable) {
        int size = 0;

        if (iterable != null) {
            for (Object object : iterable) {
                size++;
            }
        }

        return size;
    }
}
