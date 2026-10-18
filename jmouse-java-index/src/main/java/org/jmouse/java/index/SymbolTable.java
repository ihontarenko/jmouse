package org.jmouse.java.index;

import java.util.List;
import java.util.Optional;

/**
 * Where a {@link TypeResolver} looks a type up: every type declared in the sources it is resolving
 * against.
 *
 * <p>A seam rather than a class because the answer lives in different places for different
 * consumers — an editor holds its source tree in memory or in its own index files, a corpus service
 * holds it in a database — and the rules that decide what a name denotes must not care which.
 */
public interface SymbolTable {

    /**
     * Finds the types declared under one canonical name.
     *
     * <p>More than one is a real answer, not an error: two modules of one tree can declare the same
     * name, and a resolver reports that as ambiguous rather than picking one.
     *
     * @param qualifiedName the canonical name, nested types joined by dots — {@code
     *     java.util.Map.Entry}
     * @return every type declared under that name; empty when none is known
     */
    List<TypeSymbol> findTypes(String qualifiedName);

    /** A known contribution by source identity; external storage can load it lazily. */
    default Optional<FileIndex> file(String source) {
        return Optional.empty();
    }

    /** Members declared directly by a type. Default keeps declaration-only adapters compatible. */
    default List<MemberSymbol> findMembers(TypeSymbol type) {
        return List.of();
    }

    /** Direct resolved supertypes, used for inherited members. */
    default List<TypeSymbol> supertypes(TypeSymbol type) {
        return List.of();
    }

    /** The declaration context of a type (needed to resolve its members' written signatures). */
    default Optional<FileIndex> declaringFile(TypeSymbol type) {
        return Optional.empty();
    }
}
