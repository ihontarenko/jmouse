package org.jmouse.el.type;

import java.util.Objects;

/**
 * A type that is its carrier and nothing more: {@code int} is a {@link Integer}, {@code string} a {@link String},
 * {@code any} whatever is given.
 *
 * @param name    the name a document writes
 * @param carrier the Java class a value of it is
 */
public record CarrierType(String name, Class<?> carrier) implements ValueType {

    public CarrierType {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(carrier, "carrier");
    }

    @Override
    public String toString() {
        return name;
    }
}
