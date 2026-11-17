package org.jmouse.cad;

import java.util.Optional;

/**
 * A name a part is set by — {@code resistance = R}, {@code bytes = "8 0"} —, or one standing alone, {@code closed}.
 *
 * @param name  the parameter it sets, or the word that stands alone
 * @param value what it is set to: a formula, or a text
 */
public record Setting(String name, Given value) {

    public Optional<Given> given() {
        return Optional.ofNullable(value);
    }
}
