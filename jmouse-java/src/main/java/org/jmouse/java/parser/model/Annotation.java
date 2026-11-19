package org.jmouse.java.parser.model;

import org.jmouse.java.parser.source.SourceSpan;

/**
 * An annotation use, as written. Nothing is resolved or evaluated.
 *
 * @param name      the annotation's name as written, for example {@code Override} or {@code jakarta.annotation.Nullable}
 * @param arguments the exact text between the parentheses, or {@code null} for a marker annotation without them
 * @param span      from the {@code @} to the closing parenthesis or the last name segment
 */
public record Annotation(String name, String arguments, SourceSpan span) {

    /**
     * Validates the required components.
     */
    public Annotation {
        if (name == null || name.isBlank() || span == null) {
            throw new IllegalArgumentException("An annotation needs a name and a span.");
        }
    }
}
