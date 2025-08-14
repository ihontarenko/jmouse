package org.jmouse.util;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.sort;

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
     * Removes duplicate elements from the given array using sorting.
     * This method is optimized for arrays of {@link Comparable} elements.
     * <p>
     * The input array is modified during processing, and the resulting unique elements
     * are returned in a new array.
     * <p>
     * <b>Time Complexity:</b> O(n log n) due to sorting, followed by a single pass O(n)
     * to filter out duplicates, making the overall complexity O(n log n).
     *
     * @param <T> the type of elements in the array, must implement {@link Comparable}
     * @param array the input array to process
     * @return a new array containing only unique elements from the input array, preserving order
     */
    public static <T extends Comparable<T>> T[] unique(T[] array) {
        if (array == null || array.length <= 1) {
            return array;
        }

        // Sort the array to bring duplicates together
        sort(array);

        int unique = 0;
        for (int i = 1; i < array.length; i++) {
            if (!array[i].equals(array[i - 1])) {
                array[unique++] = array[i - 1];
            }
        }

        // Ensure the last unique element is included
        array[unique++] = array[array.length - 1];

        @SuppressWarnings({"unchecked"})
        T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), unique);

        System.arraycopy(array, 0, result, 0, unique);

        return result;
    }

    /**
     * Removes duplicate elements from the given array using a nested loop comparison.
     * This method is suitable for arrays of arbitrary structured types that do not implement {@link Comparable}.
     * <p>
     * This method has a higher time complexity due to its use of a nested loop
     * for duplicate detection, making it less efficient for large datasets.
     * <p>
     * <b>Time Complexity:</b> Worst-case complexity is O(n²), but in practice,
     * the actual complexity depends on the number of unique elements, `u`.
     * The inner loop runs at most `u` times, making the expected complexity O(n * u),
     * where `u` is the number of unique elements (u ≤ n).
     *
     * @param <T> the type of elements in the array
     * @param array the input array to process
     * @return a new array containing only unique elements from the input array, preserving order
     */
    public static <T> T[] unique(T[] array) {
        if (array == null || array.length <= 1) {
            return array;
        }

        int unique = 0;

        @SuppressWarnings("unchecked")
        T[] temporary = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length);

        for (T t : array) {
            boolean duplicate = false;

            for (int j = 0; j < unique; j++) {
                if (temporary[j].equals(t)) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate) {
                temporary[unique++] = t;
            }
        }

        @SuppressWarnings({"unchecked"})
        T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), unique);

        System.arraycopy(temporary, 0, result, 0, unique);

        return result;
    }

    /**
     * 🔍 Finds all indices of the target value in the given array.
     *
     * @param array  source array
     * @param target value to search for
     * @return array of matching indices
     */
    public static int[] search(Object[] array, Object target) {
        Set<Integer> indices = new HashSet<>();

        if (array != null && array.length > 0) {

            // 🎯 Fast path: single-element array always returns [0]
            if (array.length == 1) {
                return new int[]{0};
            }

            int index = 0;

            // 🔁 Iterate and match target
            for (Object object : array) {
                if (object != null && object.equals(target)) {
                    indices.add(index);
                }
                index++;
            }
        }

        // 📦 Convert to int[]
        return indices.stream().mapToInt(Integer::intValue).toArray();
    }


    /**
     * Expands an array to a new size, appending default values as needed.
     * <p>
     * This method works with both primitive and structured arrays. If the new size is less than or equal to the
     * original array size, the original array is returned unchanged. If the array is expanded, the additional
     * elements will be initialized to their default values (e.g., 0 for integers, null for objects).
     * </p>
     *
     * @param <T>      the type of the array (can be a primitive or structured array)
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

    public static <T> T[] reverse(T[] a) {
        T[] b = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length);
        int n = a.length;
        int j = n;

        for (int i = 0; i < n; i++) {
            b[--j] = a[i];
        }

        return b;
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
     * Retrieves an element from the specified array at the given index.
     * <p>
     * If the index is out of bounds or the array is null/empty, the provided default value is returned.
     * </p>
     *
     * @param array        the array from which to retrieve the element
     * @param index        the index of the element to retrieve
     * @param defaultValue the value to return if the index is out of bounds or the array is null/empty
     * @param <T>          the type of the array elements
     * @return the element at the specified index, or {@code defaultValue} if unavailable
     */
    public static <T> T get(T[] array, int index, T defaultValue) {
        T value = defaultValue;

        if (notEmpty(array) && index >= 0 && index < array.length) {
            value = array[index];
        }

        return value;
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
