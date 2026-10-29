package org.jmouse.cad;

import java.util.Objects;

/** A text a name is given — {@code operation = "write"} —: what it says, without its quotes. */
public record Text(String text) implements Given {

    public Text {
        Objects.requireNonNull(text, "text");
    }
}
