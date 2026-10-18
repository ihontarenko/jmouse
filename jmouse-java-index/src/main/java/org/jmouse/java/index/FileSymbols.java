package org.jmouse.java.index;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Small search contribution that can be restored without loading expressions or full declarations.
 */
public record FileSymbols(
        String source, List<TypeSymbol> types, Set<String> typeNames, Set<String> memberNames) {
    public FileSymbols {
        types = List.copyOf(types);
        typeNames = Set.copyOf(typeNames);
        memberNames = Set.copyOf(memberNames);
    }

    public static FileSymbols of(FileIndex file) {
        List<TypeSymbol> types =
                file.types().stream()
                        .map(
                                type ->
                                        new TypeSymbol(
                                                type.symbolId(),
                                                type.qualifiedName(),
                                                type.kind(),
                                                file.source(),
                                                type.nameSpan()))
                        .toList();
        Set<String> typeNames =
                file.references().stream()
                        .map(IndexedReference::simpleName)
                        .collect(Collectors.toSet());
        Set<String> memberNames =
                file.bodies().stream()
                        .flatMap(body -> body.references().stream())
                        .map(reference -> reference.name())
                        .collect(Collectors.toSet());
        return new FileSymbols(file.source(), types, typeNames, memberNames);
    }
}
