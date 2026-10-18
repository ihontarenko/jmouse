package org.jmouse.java.index;

import org.jmouse.java.parser.model.BodyReference;
import org.jmouse.java.parser.model.ExecutableBody;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeReferenceRole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An in-memory index of one source tree: the {@link FileIndex} of every file, and the questions an
 * editor asks of all of them together.
 *
 * <p>No global graph is kept. What is stored is per file plus two lookup tables — canonical name to
 * types, and written simple name to the files that write it. A cross-file question first narrows
 * the files by name, then resolves only the references in them, so the cost of an answer follows
 * how often a name is written, not how big the tree is. Replacing one file touches only that file's
 * rows.
 *
 * <p>Safe to share between threads: reads run concurrently, a {@link #put} or {@link #remove} waits
 * for them.
 */
public final class CodeIndex implements SymbolTable {

    private static final Set<TypeReferenceRole> SUPERTYPE_ROLES =
            Set.of(TypeReferenceRole.EXTENDS, TypeReferenceRole.IMPLEMENTS);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, FileIndex> files = new ConcurrentHashMap<>();
    private final Map<String, FileSymbols> catalog = new HashMap<>();
    private final Function<String, FileIndex> fileLoader;
    private final Map<String, List<TypeSymbol>> typesByName = new HashMap<>();
    private final Map<String, Set<String>> sourcesByWrittenName = new HashMap<>();
    private final Map<String, Set<String>> sourcesByMemberName = new HashMap<>();
    private final TypeResolver resolver = new TypeResolver(this);
    private SymbolTable externalSymbols = name -> List.of();

    /**
     * Replaces the external classpath as one ordered change; project declarations take precedence.
     */
    public void externalSymbols(SymbolTable symbols) {
        write(
                () -> {
                    externalSymbols = Objects.requireNonNull(symbols);
                    return null;
                });
    }

    public CodeIndex() {
        this(List.of(), source -> null);
    }

    /**
     * Restores search metadata immediately; full file contributions are loaded only when queried.
     */
    public CodeIndex(List<FileSymbols> catalog, Function<String, FileIndex> fileLoader) {
        this.fileLoader = Objects.requireNonNull(fileLoader);
        for (FileSymbols symbols : catalog) {
            remember(symbols);
        }
    }

    private FileIndex load(String source) {
        return catalog.containsKey(source) ? files.computeIfAbsent(source, fileLoader) : null;
    }

    private void remember(FileSymbols symbols) {
        catalog.put(symbols.source(), symbols);
        for (TypeSymbol type : symbols.types()) {
            typesByName.computeIfAbsent(type.qualifiedName(), name -> new ArrayList<>()).add(type);
        }
        for (String name : symbols.typeNames()) {
            sourcesByWrittenName
                    .computeIfAbsent(name, ignored -> new HashSet<>())
                    .add(symbols.source());
        }
        for (String name : symbols.memberNames()) {
            sourcesByMemberName
                    .computeIfAbsent(name, ignored -> new HashSet<>())
                    .add(symbols.source());
        }
    }

    /**
     * Adds a file, or replaces the one indexed under the same source name.
     *
     * @param file the file's index
     */
    public void put(FileIndex file) {
        write(
                () -> {
                    forget(file.source());
                    files.put(file.source(), file);
                    remember(FileSymbols.of(file));
                    return null;
                });
    }

    /**
     * Removes a file.
     *
     * @param source the source name it was indexed under
     */
    public void remove(String source) {
        write(
                () -> {
                    forget(source);
                    return null;
                });
    }

    /**
     * Returns one file's index.
     *
     * @param source the source name
     * @return the index, if the file is known
     */
    public Optional<FileIndex> file(String source) {
        return read(
                () ->
                        catalog.containsKey(source)
                                ? Optional.ofNullable(load(source))
                                : externalSymbols.file(source));
    }

    /**
     * Returns how many files are indexed.
     *
     * @return the count
     */
    public int size() {
        return read(catalog::size);
    }

    /** Search metadata only; never hydrates a full file contribution. */
    public Optional<FileSymbols> fileSymbols(String source) {
        return read(() -> Optional.ofNullable(catalog.get(source)));
    }

    @Override
    public List<TypeSymbol> findTypes(String qualifiedName) {
        return read(
                () ->
                        typesByName.containsKey(qualifiedName)
                                ? List.copyOf(typesByName.get(qualifiedName))
                                : externalSymbols.findTypes(qualifiedName));
    }

    /**
     * Finds a declared type by where it is declared.
     *
     * @param source the file
     * @param symbolId the type's id in that file
     * @return the type, if the file declares it
     */
    public Optional<TypeSymbol> type(String source, SymbolId symbolId) {
        return file(source)
                .flatMap(
                        file ->
                                file.declaration(symbolId)
                                        .filter(IndexedDeclaration::isType)
                                        .map(type -> symbolOf(file, type)));
    }

    /**
     * Resolves a reference written in an indexed file.
     *
     * @param source the file
     * @param reference one of its references
     * @return what the name denotes
     */
    public Resolution resolve(String source, IndexedReference reference) {
        return read(() -> resolver.resolve(file(source).orElse(null), reference));
    }

    /**
     * Searches types by their simple name, the way "go to class" does.
     *
     * @param pattern a prefix, camel humps such as {@code BeBi}, or a lower-case fragment
     * @param limit the most results to return
     * @return the best matches first
     */
    public List<TypeSymbol> searchTypes(String pattern, int limit) {
        return read(
                () -> {
                    record Scored(TypeSymbol type, int score) {}
                    List<Scored> scored = new ArrayList<>();
                    for (List<TypeSymbol> types : typesByName.values()) {
                        for (TypeSymbol type : types) {
                            int score = TypeNameMatcher.score(pattern, type.simpleName());
                            if (score != TypeNameMatcher.NO_MATCH) {
                                scored.add(new Scored(type, score));
                            }
                        }
                    }
                    return scored.stream()
                            .sorted(
                                    Comparator.comparingInt(Scored::score)
                                            .reversed()
                                            .thenComparing(entry -> entry.type().qualifiedName()))
                            .limit(limit)
                            .map(Scored::type)
                            .toList();
                });
    }

    /**
     * Finds every place a type is named in a declaration header.
     *
     * @param type the type
     * @return the usages, grouped by file, in source order within a file
     */
    public List<Usage> usages(TypeSymbol type) {
        return read(
                () -> {
                    List<Usage> usages = new ArrayList<>();
                    for (String source : candidateSources(type)) {
                        FileIndex file = load(source);
                        for (IndexedReference reference : file.references()) {
                            if (reference.simpleName().equals(type.simpleName())
                                    && denotes(resolver.resolve(file, reference), type)) {
                                usages.add(new Usage(source, reference));
                            }
                        }
                    }
                    return usages;
                });
    }

    /**
     * Finds the types that name this one in their {@code extends} or {@code implements} clause.
     *
     * @param type the supertype
     * @return the direct subtypes, by canonical name
     */
    public List<TypeSymbol> directSubtypes(TypeSymbol type) {
        return read(
                () -> {
                    List<TypeSymbol> subtypes = new ArrayList<>();
                    for (String source : candidateSources(type)) {
                        FileIndex file = load(source);
                        for (IndexedReference reference : file.references()) {
                            if (SUPERTYPE_ROLES.contains(reference.role())
                                    && reference.simpleName().equals(type.simpleName())
                                    && denotes(resolver.resolve(file, reference), type)) {
                                file.declaration(reference.owner())
                                        .ifPresent(owner -> subtypes.add(symbolOf(file, owner)));
                            }
                        }
                    }
                    subtypes.sort(Comparator.comparing(TypeSymbol::qualifiedName));
                    return subtypes;
                });
    }

    /**
     * Finds every type below this one, through any number of {@code extends} and {@code implements}
     * steps.
     *
     * @param type the supertype
     * @return all subtypes, each once, nearest first
     */
    public List<TypeSymbol> allSubtypes(TypeSymbol type) {
        List<TypeSymbol> found = new ArrayList<>();
        List<TypeSymbol> pending = new ArrayList<>(directSubtypes(type));
        while (!pending.isEmpty()) {
            TypeSymbol next = pending.removeFirst();
            if (!found.contains(next)) {
                found.add(next);
                pending.addAll(directSubtypes(next));
            }
        }
        return found;
    }

    private List<String> candidateSources(TypeSymbol type) {
        return sourcesByWrittenName.getOrDefault(type.simpleName(), Set.of()).stream()
                .sorted()
                .toList();
    }

    private static boolean denotes(Resolution resolution, TypeSymbol type) {
        return switch (resolution) {
            case Resolution.Resolved resolved -> resolved.target().equals(type);
            case Resolution.Ambiguous ambiguous -> ambiguous.candidates().contains(type);
            case Resolution.TypeVariable ignored -> false;
            case Resolution.Unresolved ignored -> false;
        };
    }

    /** Members declared directly by a known type. */
    @Override
    public List<MemberSymbol> findMembers(TypeSymbol type) {
        return read(
                () ->
                        !catalog.containsKey(type.source())
                                ? externalSymbols.findMembers(type)
                                : file(type.source())
                                        .map(
                                                file ->
                                                        file.members().stream()
                                                                .filter(
                                                                        member ->
                                                                                member.container()
                                                                                        .equals(
                                                                                                type
                                                                                                        .symbolId()))
                                                                .toList())
                                        .orElse(List.of()));
    }

    @Override
    public Optional<FileIndex> declaringFile(TypeSymbol type) {
        return file(type.source());
    }

    @Override
    public List<TypeSymbol> supertypes(TypeSymbol type) {
        return read(
                () ->
                        !catalog.containsKey(type.source())
                                ? externalSymbols.supertypes(type)
                                : file(type.source())
                                        .map(
                                                file ->
                                                        file.references().stream()
                                                                .filter(
                                                                        reference ->
                                                                                type.symbolId()
                                                                                                .equals(
                                                                                                        reference
                                                                                                                .owner())
                                                                                        && SUPERTYPE_ROLES
                                                                                                .contains(
                                                                                                        reference
                                                                                                                .role()))
                                                                .map(
                                                                        reference ->
                                                                                resolver.resolve(
                                                                                        file,
                                                                                        reference))
                                                                .filter(
                                                                        Resolution.Resolved.class
                                                                                ::isInstance)
                                                                .map(
                                                                        Resolution.Resolved.class
                                                                                ::cast)
                                                                .map(Resolution.Resolved::target)
                                                                .toList())
                                        .orElse(List.of()));
    }

    /** Resolves a body reference without guessing when its receiver or arguments are unknown. */
    public MemberResolution resolveMember(String source, BodyReference reference) {
        return read(() -> new MemberResolver(this).resolve(file(source).orElse(null), reference));
    }

    /** Finds definite member usages; ambiguous candidates are not reported as definite usages. */
    public List<MemberUsage> usages(MemberSymbol member) {
        return read(
                () -> {
                    List<MemberUsage> usages = new ArrayList<>();
                    MemberResolver members = new MemberResolver(this);
                    for (String source :
                            sourcesByMemberName.getOrDefault(member.name(), Set.of()).stream()
                                    .sorted()
                                    .toList()) {
                        FileIndex file = load(source);
                        for (ExecutableBody body : file.bodies()) {
                            for (BodyReference reference : body.references()) {
                                if (reference.name().equals(member.name())
                                        && members.resolve(file, reference)
                                                instanceof MemberResolution.Resolved resolved
                                        && resolved.target().equals(member)) {
                                    usages.add(new MemberUsage(source, reference));
                                }
                            }
                        }
                    }
                    return List.copyOf(usages);
                });
    }

    private void forget(String source) {
        files.remove(source);
        FileSymbols previous = catalog.remove(source);
        if (previous == null) {
            return;
        }
        for (TypeSymbol type : previous.types()) {
            List<TypeSymbol> types = typesByName.get(type.qualifiedName());
            if (types != null) {
                types.remove(type);
                if (types.isEmpty()) {
                    typesByName.remove(type.qualifiedName());
                }
            }
        }
        forgetNames(previous.typeNames(), source, sourcesByWrittenName);
        forgetNames(previous.memberNames(), source, sourcesByMemberName);
    }

    private static void forgetNames(
            Set<String> names, String source, Map<String, Set<String>> candidates) {
        for (String name : names) {
            Set<String> sources = candidates.get(name);
            if (sources != null) {
                sources.remove(source);
                if (sources.isEmpty()) {
                    candidates.remove(name);
                }
            }
        }
    }

    private static TypeSymbol symbolOf(FileIndex file, IndexedDeclaration type) {
        return new TypeSymbol(
                type.symbolId(), type.qualifiedName(), type.kind(), file.source(), type.nameSpan());
    }

    private <T> T read(Supplier<T> action) {
        lock.readLock().lock();
        try {
            return action.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    private <T> T write(Supplier<T> action) {
        lock.writeLock().lock();
        try {
            return action.get();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
