package org.jmouse.core.proxy2.support;

public final class Exceptions {

    private Exceptions() {
    }

    public static RuntimeException launder(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }

        if (throwable instanceof Error error) {
            return new RuntimeException(error);
        }

        return new RuntimeException(throwable);
    }

}
