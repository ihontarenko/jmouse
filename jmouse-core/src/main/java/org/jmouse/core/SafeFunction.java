package org.jmouse.core;

import java.util.function.Function;

public interface SafeFunction<T, R> {
    R apply(T value);

    static <T, R> SafeFunction<T, R> safeApply(Function<T, R> unsafeFunction) {
        return value -> {
            try {
                return unsafeFunction.apply(value);
            } catch (Exception ignored) { }
            return null;
        };
    }

}
