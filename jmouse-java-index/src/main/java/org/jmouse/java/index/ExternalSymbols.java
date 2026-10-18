package org.jmouse.java.index;

import org.jmouse.java.parser.model.TypeReferenceRole;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Lazy external declarations. The caller supplies storage; this module performs no I/O or class
 * loading.
 */
public final class ExternalSymbols implements SymbolTable {

    private final Function<String, Optional<FileIndex>> lookup;
    private final ConcurrentHashMap<String, Optional<FileIndex>> byName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FileIndex> bySource = new ConcurrentHashMap<>();
    private final TypeResolver resolver = new TypeResolver(this);

    public ExternalSymbols(Function<String, Optional<FileIndex>> lookup) {
        this.lookup = Objects.requireNonNull(lookup);
    }

    @Override
    public List<TypeSymbol> findTypes(String qualifiedName) {
        Optional<FileIndex> contribution =
                byName.computeIfAbsent(
                        qualifiedName,
                        name -> {
                            Optional<FileIndex> file = lookup.apply(name);
                            file.ifPresent(index -> bySource.put(index.source(), index));
                            return file;
                        });
        return contribution
                .map(
                        file ->
                                FileSymbols.of(file).types().stream()
                                        .filter(type -> type.qualifiedName().equals(qualifiedName))
                                        .toList())
                .orElse(List.of());
    }

    @Override
    public Optional<FileIndex> file(String source) {
        return Optional.ofNullable(bySource.get(source));
    }

    @Override
    public Optional<FileIndex> declaringFile(TypeSymbol type) {
        return file(type.source());
    }

    @Override
    public List<MemberSymbol> findMembers(TypeSymbol type) {
        return file(type.source())
                .map(
                        file ->
                                file.members().stream()
                                        .filter(
                                                member ->
                                                        member.container().equals(type.symbolId()))
                                        .toList())
                .orElse(List.of());
    }

    @Override
    public List<TypeSymbol> supertypes(TypeSymbol type) {
        return file(type.source())
                .map(
                        file ->
                                file.references().stream()
                                        .filter(
                                                reference ->
                                                        type.symbolId().equals(reference.owner())
                                                                && (reference.role()
                                                                                == TypeReferenceRole
                                                                                        .EXTENDS
                                                                        || reference.role()
                                                                                == TypeReferenceRole
                                                                                        .IMPLEMENTS))
                                        .map(reference -> resolver.resolve(file, reference))
                                        .filter(Resolution.Resolved.class::isInstance)
                                        .map(Resolution.Resolved.class::cast)
                                        .map(Resolution.Resolved::target)
                                        .toList())
                .orElse(List.of());
    }
}
