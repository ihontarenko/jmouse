package org.jmouse.java.index.jpa;

import jakarta.persistence.EntityManager;

import org.jmouse.java.index.FileIndex;
import org.jmouse.java.index.FileSymbols;
import org.jmouse.java.index.MemberSymbol;
import org.jmouse.java.index.Resolution;
import org.jmouse.java.index.SymbolTable;
import org.jmouse.java.index.TypeResolver;
import org.jmouse.java.index.TypeSymbol;
import org.jmouse.java.index.store.IndexStore;
import org.jmouse.java.index.store.disk.IndexBinaryCodec;
import org.jmouse.java.parser.JavaParser;
import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeReferenceRole;
import org.jmouse.java.parser.source.SourcePosition;
import org.jmouse.java.parser.source.SourceSpan;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Scoped JPA storage and Java lookup. No commits, authorization, classpath discovery or product
 * jobs.
 */
public final class JpaIndexStore implements IndexStore, SymbolTable {
    private static final int MAXIMUM_PREFETCH_BATCH = 1000;
    private static final int MAXIMUM_SNAPSHOT_CONTEXTS = 4096;
    private final EntityManager entityManager;
    private final String scopeId;
    private final IndexConfiguration configuration;
    private final Map<String, FileIndex> prefetched = new HashMap<>();
    private final Map<String, List<TypeSymbol>> snapshotTypes = new HashMap<>();
    private boolean snapshotReads;

    public JpaIndexStore(
            EntityManager entityManager, String scopeId, IndexConfiguration configuration) {
        this.entityManager = Objects.requireNonNull(entityManager);
        this.scopeId = Objects.requireNonNull(scopeId);
        this.configuration = Objects.requireNonNull(configuration);
        IndexScopeRow scope = entityManager.find(IndexScopeRow.class, scopeId);
        if (scope == null || !scope.configuration.equals(configuration.key())) {
            throw new IllegalArgumentException(
                    "Unknown scope or incompatible Java index configuration.");
        }
    }

    @Override
    public Optional<FileIndex> read(String source) {
        IndexIdentities.requireSource(source);
        if (prefetched.containsKey(source)) {
            return Optional.of(prefetched.get(source));
        }
        return entry(source)
                .map(
                        entry ->
                                decode(
                                        entityManager.find(
                                                ContributionRow.class, entry.contributionId),
                                        source));
    }

    @Override
    public Optional<FileIndex> file(String source) {
        return read(source);
    }

    @Override
    public List<FileSymbols> catalog() {
        return entityManager
                .createQuery(
                        "SELECT entry.source, contribution.catalog FROM ScopeEntryRow entry,"
                                + " ContributionRow contribution WHERE entry.scopeId = :scope AND"
                                + " entry.contributionId = contribution.id ORDER BY entry.source",
                        Object[].class)
                .setParameter("scope", scopeId)
                .getResultList()
                .stream()
                .map(
                        row -> {
                            try {
                                FileSymbols catalog =
                                        IndexBinaryCodec.decodeCatalog((byte[]) row[1]);
                                return new FileSymbols(
                                        (String) row[0],
                                        catalog.types().stream()
                                                .map(
                                                        type ->
                                                                new TypeSymbol(
                                                                        type.symbolId(),
                                                                        type.qualifiedName(),
                                                                        type.kind(),
                                                                        (String) row[0],
                                                                        type.nameSpan()))
                                                .toList(),
                                        catalog.typeNames(),
                                        catalog.memberNames());
                            } catch (IOException failure) {
                                throw new UncheckedIOException(
                                        "Invalid Java index catalog.", failure);
                            }
                        })
                .toList();
    }

    @Override
    public void write(FileIndex file, String contentHash) {
        IndexIdentities.requireContentHash(contentHash);
        IndexIdentities.requireSource(file.source());
        if (!configuration.equals(IndexConfiguration.current(configuration.level()))) {
            throw new IllegalStateException(
                    "Writing requires the current parser/index/payload revisions.");
        }
        IndexScopeRow scope = mutableScope();
        if (configuration.level() == JavaParser.Level.DECLARATIONS && !file.bodies().isEmpty()) {
            throw new IllegalArgumentException(
                    "Expression facts cannot be written to a declarations-only scope.");
        }
        String contributionId = IndexIdentities.hash(configuration.key() + ":" + contentHash);
        ContributionRow contribution = entityManager.find(ContributionRow.class, contributionId);
        FileIndex neutral = file.withSource("");
        if (contribution == null) {
            contribution = new ContributionRow();
            contribution.id = contributionId;
            contribution.contentHash = contentHash;
            contribution.configuration = configuration.key();
            try {
                contribution.payload = IndexBinaryCodec.encodeFile(neutral);
                contribution.catalog = IndexBinaryCodec.encodeCatalog(FileSymbols.of(neutral));
            } catch (IOException failure) {
                throw new UncheckedIOException("Cannot encode Java index.", failure);
            }
            entityManager.persist(contribution);
            IndexProjections.write(entityManager, contributionId, neutral);
            entityManager.detach(contribution);
        } else if (!decode(contribution, file.source()).equals(file)) {
            throw new IllegalArgumentException(
                    "Content hash/configuration already identifies a different Java index.");
        }
        ScopeEntryRow entry = entry(file.source()).orElse(null);
        if (entry != null && entry.contributionId.equals(contributionId)) {
            return;
        }
        if (entry == null) {
            entry = new ScopeEntryRow();
            entry.id = IndexIdentities.hash(scopeId + ":" + IndexIdentities.hash(file.source()));
            entry.scopeId = scopeId;
            entry.source = file.source();
            entry.sourceHash = IndexIdentities.hash(file.source());
            entry.contributionId = contributionId;
            entityManager.persist(entry);
        } else {
            entry.contributionId = contributionId;
        }
        scope.revision++;
    }

    @Override
    public void remove(String source) {
        IndexScopeRow scope = mutableScope();
        entry(source)
                .ifPresent(
                        entry -> {
                            entityManager.remove(entry);
                            scope.revision++;
                        });
    }

    @Override
    public long revision() {
        return entityManager
                .createQuery(
                        "SELECT scope.revision FROM IndexScopeRow scope WHERE scope.id = :scope",
                        Long.class)
                .setParameter("scope", scopeId)
                .getSingleResult();
    }

    @Override
    public List<TypeSymbol> findTypes(String qualifiedName) {
        if (snapshotTypes.containsKey(qualifiedName)) {
            return snapshotTypes.get(qualifiedName);
        }
        List<TypeSymbol> result =
                entityManager
                        .createQuery(
                                "SELECT declaration, entry.source FROM DeclarationRow declaration,"
                                    + " ScopeEntryRow entry WHERE entry.scopeId = :scope AND"
                                    + " entry.contributionId = declaration.contributionId AND"
                                    + " declaration.type = true AND declaration.nameHash = :hash"
                                    + " AND declaration.qualifiedName = :name ORDER BY"
                                    + " entry.source, declaration.number",
                                Object[].class)
                        .setParameter("scope", scopeId)
                        .setParameter("hash", IndexIdentities.hash(qualifiedName))
                        .setParameter("name", qualifiedName)
                        .getResultList()
                        .stream()
                        .map(
                                row -> {
                                    DeclarationRow declaration = (DeclarationRow) row[0];
                                    return new TypeSymbol(
                                            new SymbolId(declaration.symbolId),
                                            declaration.qualifiedName,
                                            DeclarationKind.valueOf(declaration.kind),
                                            (String) row[1],
                                            new SourceSpan(
                                                    new SourcePosition(
                                                            declaration.startOffset,
                                                            declaration.startLine,
                                                            declaration.startColumn),
                                                    new SourcePosition(
                                                            declaration.endOffset,
                                                            declaration.endLine,
                                                            declaration.endColumn)));
                                })
                        .toList();
        if (snapshotReads && snapshotTypes.size() < MAXIMUM_SNAPSHOT_CONTEXTS) {
            snapshotTypes.put(qualifiedName, result);
        }
        return result;
    }

    @Override
    public List<MemberSymbol> findMembers(TypeSymbol type) {
        return read(type.source())
                .map(
                        file ->
                                file.members().stream()
                                        .filter(
                                                member ->
                                                        type.symbolId().equals(member.container()))
                                        .toList())
                .orElse(List.of());
    }

    @Override
    public Optional<FileIndex> declaringFile(TypeSymbol type) {
        return read(type.source());
    }

    @Override
    public List<TypeSymbol> supertypes(TypeSymbol type) {
        return read(type.source())
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
                                        .map(
                                                reference ->
                                                        new TypeResolver(this)
                                                                .resolve(file, reference))
                                        .filter(Resolution.Resolved.class::isInstance)
                                        .map(Resolution.Resolved.class::cast)
                                        .map(Resolution.Resolved::target)
                                        .toList())
                .orElse(List.of());
    }

    /**
     * Batch load a bounded set only on a sealed snapshot; mutable scopes never retain a stale
     * cache.
     */
    public void prefetch(List<String> sources) {
        if (!entityManager.find(IndexScopeRow.class, scopeId).sealed) {
            throw new IllegalStateException("Prefetch requires a sealed scope.");
        }
        snapshotReads = true;
        if (sources.size() > MAXIMUM_PREFETCH_BATCH) {
            throw new IllegalArgumentException("Prefetch is limited to 1000 sources per call.");
        }
        long additional =
                sources.stream()
                        .distinct()
                        .filter(source -> !prefetched.containsKey(source))
                        .count();
        if (prefetched.size() + additional > MAXIMUM_SNAPSHOT_CONTEXTS) {
            throw new IllegalArgumentException(
                    "Snapshot context cache is limited to 4096 sources.");
        }
        if (sources.isEmpty()) {
            return;
        }
        entityManager
                .createQuery(
                        "SELECT entry.source, contribution FROM ScopeEntryRow entry,"
                                + " ContributionRow contribution WHERE entry.scopeId = :scope AND"
                                + " entry.contributionId = contribution.id AND entry.source IN"
                                + " :sources",
                        Object[].class)
                .setParameter("scope", scopeId)
                .setParameter("sources", sources)
                .getResultList()
                .forEach(
                        row ->
                                prefetched.put(
                                        (String) row[0],
                                        decode((ContributionRow) row[1], (String) row[0])));
    }

    /** Exact written-name candidates, before semantic resolution; bounded and scope-isolated. */
    public List<String> referencingSources(String writtenName, int limit) {
        if (writtenName == null || writtenName.isBlank() || limit < 1 || limit > 10000) {
            throw new IllegalArgumentException(
                    "A written name and a candidate limit from 1 to 10000 are required.");
        }
        return entityManager
                .createQuery(
                        "SELECT DISTINCT entry.source FROM ScopeEntryRow entry, ReferenceRow"
                            + " reference WHERE entry.scopeId = :scope AND entry.contributionId ="
                            + " reference.contributionId AND reference.targetHash = :hash AND"
                            + " reference.target = :name ORDER BY entry.source",
                        String.class)
                .setParameter("scope", scopeId)
                .setParameter("hash", IndexIdentities.hash(writtenName))
                .setParameter("name", writtenName)
                .setMaxResults(limit)
                .getResultList();
    }

    /** Opaque contribution id for product bridge rows; it does not grant permission to read it. */
    public Optional<String> contributionId(String source) {
        return entry(source).map(entry -> entry.contributionId);
    }

    private Optional<ScopeEntryRow> entry(String source) {
        IndexIdentities.requireSource(source);
        return entityManager
                .createQuery(
                        "SELECT entry FROM ScopeEntryRow entry WHERE entry.scopeId = :scope AND"
                                + " entry.sourceHash = :hash",
                        ScopeEntryRow.class)
                .setParameter("scope", scopeId)
                .setParameter("hash", IndexIdentities.hash(source))
                .getResultStream()
                .findFirst()
                .map(
                        entry -> {
                            if (!entry.source.equals(source)) {
                                throw new IllegalStateException("Source identity hash collision.");
                            }
                            return entry;
                        });
    }

    private IndexScopeRow mutableScope() {
        IndexScopeRow scope = new JpaIndexScopes(entityManager).locked(scopeId);
        if (scope.sealed) {
            throw new IllegalStateException("A sealed Java index scope is immutable.");
        }
        return scope;
    }

    private FileIndex decode(ContributionRow contribution, String source) {
        if (contribution == null || !configuration.key().equals(contribution.configuration)) {
            throw new IllegalStateException("Missing or incompatible Java index contribution.");
        }
        try {
            return IndexBinaryCodec.decodeFile(contribution.payload).withSource(source);
        } catch (IOException failure) {
            throw new UncheckedIOException("Invalid Java index payload.", failure);
        }
    }
}
