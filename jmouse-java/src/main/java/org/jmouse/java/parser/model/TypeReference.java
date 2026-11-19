package org.jmouse.java.parser.model;

import org.jmouse.java.parser.source.SourceSpan;

/**
 * One type name as it is written in a declaration header, with the exact place it is written.
 *
 * <p>The name is never resolved: {@code List} and {@code java.util.List} stay two different texts, and a
 * type parameter such as {@code T} is reported like any other name. Deciding what a name denotes is the
 * job of whoever holds the other files.
 *
 * <p>Type arguments and type-use annotations are not part of the name — each is a reference of its own —
 * so {@code Map.@NonNull Entry<String, Value>} gives {@code Map.Entry}, {@code NonNull}, {@code String} and
 * {@code Value}. A qualified name covers its whole written extent: for {@code Outer<String>.Inner} the
 * name is {@code Outer.Inner} and the span runs from {@code Outer} to {@code Inner}.
 *
 * @param name the dotted name as written, type arguments and annotations removed
 * @param role the clause the name is written in
 * @param span from the first identifier of the name to the end of its last
 */
public record TypeReference(String name, TypeReferenceRole role, SourceSpan span) {

    /**
     * Validates the components.
     */
    public TypeReference {
        if (name == null || name.isBlank() || role == null || span == null) {
            throw new IllegalArgumentException("A type reference needs a name, a role and a span.");
        }
    }

    /**
     * Returns the last segment of the name — what an unqualified reader would call the type.
     *
     * @return {@code Entry} for {@code Map.Entry}
     */
    public String simpleName() {
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 ? name : name.substring(lastDot + 1);
    }

    /**
     * Returns whether the name is written with a qualifier.
     *
     * @return {@code true} for {@code java.util.List} and for {@code Map.Entry}
     */
    public boolean isQualified() {
        return name.indexOf('.') >= 0;
    }
}
