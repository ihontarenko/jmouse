package org.jmouse.core.cache;

import java.util.Objects;

/**
 * 🧱 Optional value wrapper to centralize hashing/equality semantics for keys.
 */
public record KeyWrapper<K>(K value) {

    public static <K> KeyWrapper<K> of(K value) {
        return new KeyWrapper<>(value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj) || (obj instanceof KeyWrapper<?>(Object value1) && Objects.equals(value, value1));
    }
}
