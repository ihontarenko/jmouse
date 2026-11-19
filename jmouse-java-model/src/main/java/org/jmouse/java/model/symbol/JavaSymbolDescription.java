package org.jmouse.java.model.symbol;

import org.jmouse.java.model.type.JavaType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A symbol's semantic shape. Member and supertype lists are immutable and empty when not
 * applicable.
 */
public record JavaSymbolDescription(
        JavaSymbolKey key,
        JavaType type,
        Set<JavaModifier> modifiers,
        List<JavaSymbolKey> members,
        List<JavaType> supertypes,
        JavaSymbolOriginKind origin,
        Optional<JavaSourceLocation> source) {
    public JavaSymbolDescription {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        modifiers = Set.copyOf(modifiers);
        members = List.copyOf(members);
        supertypes = List.copyOf(supertypes);
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(source, "source");
    }
}
