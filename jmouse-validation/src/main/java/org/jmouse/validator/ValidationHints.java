package org.jmouse.validator;

import java.util.Arrays;
import java.util.List;

public record ValidationHints(List<Object> values) {

    public static ValidationHints empty() {
        return new ValidationHints(List.of());
    }

    public static ValidationHints of(Object... hints) {
        if (hints == null || hints.length == 0) {
            return empty();
        }
        return new ValidationHints(Arrays.asList(hints));
    }

    public boolean isEmpty() {
        return values == null || values.isEmpty();
    }

    public boolean has(Class<?> type) {
        if (values == null) {
            return false;
        }

        for (Object value : values) {
            if (type.isInstance(value)) {
                return true;
            }
        }

        return false;
    }

    public <T> T find(Class<T> type) {
        if (values == null) {
            return null;
        }

        for (Object value : values) {
            if (type.isInstance(value)) {
                return type.cast(value);
            };
        }

        return null;
    }

    public boolean contains(Object hint) {
        return values != null && values.contains(hint);
    }

}
