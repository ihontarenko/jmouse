package org.jmouse.util;

final public class Assert {

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertException("Assertion failed: " + message);
        }
    }

    public static void nonNull(Object object, String message) {
        if (object == null) {
            throw new AssertException("Assertion failed: " + message);
        }
    }

    static class AssertException extends RuntimeException {

        public AssertException(String message, Throwable cause) {
            super(message, cause);
        }

        public AssertException(String message) {
            super(message);
        }

        public AssertException() {

        }
    }

}
