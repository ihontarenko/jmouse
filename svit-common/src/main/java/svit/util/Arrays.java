package svit.util;

import java.lang.reflect.Array;

/**
 * Utility class for array operations.
 */
public final class Arrays {

    private Arrays() {
        // Private constructor to prevent instantiation
    }

    /**
     * Concatenates two arrays into a single array.
     * <p>
     * The resulting array contains all elements of the first array, followed by all elements of the second array.
     * </p>
     *
     * @param <T> the type of the elements in the arrays.
     * @param a   the first array to concatenate.
     * @param b   the second array to concatenate.
     * @return a new array containing all elements of {@code a} followed by all elements of {@code b}.
     *
     * <p>Example Usage:</p>
     * <pre>{@code
     * String[] array1 = {"a", "b"};
     * String[] array2 = {"c", "d"};
     * Arrays.concatenate(array1, array2); // [a, b, c, d]
     * }</pre>
     */
    public static <T> T[] concatenate(T[] a, T[] b) {
        int sizeA = a.length;
        int sizeB = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), sizeA + sizeB);

        System.arraycopy(a, 0, c, 0, sizeA);
        System.arraycopy(b, 0, c, sizeA, sizeB);

        return c;
    }

    /**
     * Checks if the given array is null or empty.
     *
     * @param array the array to check
     * @return {@code true} if the array is null or has no elements, {@code false} otherwise
     */
    public static boolean empty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Checks if the given array is not null and contains at least one element.
     *
     * @param array the array to check
     * @return {@code true} if the array is not null and contains elements, {@code false} otherwise
     */
    public static boolean notEmpty(Object[] array) {
        return !empty(array);
    }

}
