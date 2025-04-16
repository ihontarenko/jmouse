package org.jmouse.util.helper;

/**
 * A utility class that provides helper methods for working with iterables.
 * <p>
 * This class contains static methods and is not meant to be instantiated.
 * </p>
 */
public class Iterations {

    /**
     * Private constructor to prevent instantiation.
     */
    private Iterations() {
        // Prevent instantiation of this utility class.
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

        for (Object object : iterable) {
            size++;
        }

        return size;
    }
}
