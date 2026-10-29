package org.jmouse.cad;

import java.util.Optional;

/** A name a part is set by — {@code resistance = R}, {@code bytes = "8 0"} —, or one standing alone, {@code closed}. */
public record Setting(String name, Given value) {

    public Optional<Given> given() {
        return Optional.ofNullable(value);
    }
}
