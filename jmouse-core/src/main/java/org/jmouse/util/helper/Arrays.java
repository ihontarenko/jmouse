package org.jmouse.util.helper;

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

        @SuppressWarnings("unchecked") T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), sizeA + sizeB);

        System.arraycopy(a, 0, c, 0, sizeA);
        System.arraycopy(b, 0, c, sizeA, sizeB);

        return c;
    }

    /**
     * Expands an array to a new size, appending default values as needed.
     * <p>
     * This method works with both primitive and object arrays. If the new size is less than or equal to the
     * original array size, the original array is returned unchanged. If the array is expanded, the additional
     * elements will be initialized to their default values (e.g., 0 for integers, null for objects).
     * </p>
     *
     * @param <T>      the type of the array (can be a primitive or object array)
     * @param oldArray the original array to expand
     * @param newSize  the desired size of the new array
     * @return the expanded array with default values, or the original array if no expansion is required
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T expand(T oldArray, int newSize) {
        Class<?> arrayType = oldArray.getClass();
        T        newArray  = oldArray;

        if (arrayType.isArray()) {
            int oldSize = Array.getLength(oldArray);

            if (newSize > oldSize) {
                // Create a new array with the specified size
                newArray = (T) Array.newInstance(arrayType.getComponentType(), newSize);
                // Copy elements from the old array to the new array
                System.arraycopy(oldArray, 0, newArray, 0, oldSize);
            }
        }

        return newArray;
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

    /**
     * Converts a primitive type to its corresponding boxed (wrapper) type.
     * <p>
     * If the given class is a primitive type, this method returns the equivalent wrapper type.
     * If the input is already a reference type, it is returned unchanged.
     * </p>
     *
     * @param primitiveType the primitive class type to be boxed
     * @return the corresponding boxed type, or the original class if it is not a primitive
     * <p>
     * Example:
     * <pre>{@code
     * box(int.class);    // Integer.class
     * box(double.class); // Double.class
     * box(String.class); // String.class (unchanged)
     * }</pre>
     */
    public static Class<?> boxType(Class<?> primitiveType) {
        Class<?> boxedType = primitiveType;

        if (primitiveType.isPrimitive()) {
            Object array = Array.newInstance(primitiveType, 1);
            boxedType = Array.get(array, 0).getClass();
        }

        return boxedType;
    }

    /**
     * Converts a primitive value to its corresponding boxed (wrapper).
     * <p>
     * If the given class is a primitive type, this method returns the equivalent boxed value.
     * </p>
     *
     * @param value the primitive value (or not) type to be boxed
     * @return the corresponding boxed value, or the original value if it is not a primitive
     */
    public static Object boxValue(Object value) {
        Class<?> valueType = value.getClass();

        if (valueType.isPrimitive()) {
            Object array = Array.newInstance(valueType, 1);
            value = Array.get(array, 0).getClass();
        }

        return value;
    }

}
