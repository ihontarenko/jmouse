package org.jmouse.util;

final public class Exceptions {

    public static void thrownIfFalse(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException("Assertion failed: " + message);
        }
    }

    public static void throwIfNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException("Must be non-NULL: " + message);
        }
    }

    public static void throwIfOutOfRange(int index, int min, int max, String message) {
        if (index < min || index > max) {
            throw new IllegalArgumentException("Must be between %d and %d inclusive: %s".formatted(min, max, message));
        }
    }

    public static void throwIfOutOfRange(Object[] array, int index, String message) {
        throwIfNull(array, message);
        throwIfOutOfRange(index, 0, array.length, message);
    }

}
