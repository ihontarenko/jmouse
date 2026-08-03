package org.jmouse.storage.jpa;

import jakarta.persistence.EntityManager;

import java.util.stream.Stream;

/**
 * 🔗 A reference source that is one query.
 *
 * <p>Which is what every real one turns out to be. A product's implementations differed only in a
 * table name and a JPQL string — three of them across two products, identical in every other line —
 * and three copies of a query runner is three places for the same mistake.</p>
 *
 * <pre>{@code
 * @Bean
 * StoredFileReferences fileUploadReferences(EntityManager entityManager) {
 *     return new JpaStoredFileReferences(entityManager, "file_uploads",
 *                                        "SELECT upload.storedFile.id FROM FileUpload upload");
 * }
 * }</pre>
 *
 * <p>A source that genuinely needs more — several tables, a filter that cannot be expressed in one
 * query — implements {@link StoredFileReferences} directly. This is the common case made short, not
 * the only permitted shape.</p>
 */
public class JpaStoredFileReferences implements StoredFileReferences {

    private final EntityManager entityManager;
    private final String        sourceName;
    private final String        query;

    /**
     * 🏗️ Report the identifiers one query returns.
     *
     * @param entityManager persistence context to query in
     * @param sourceName    what this source is called in a sweep report
     * @param query         JPQL selecting registry identifiers; must return {@code String}
     */
    public JpaStoredFileReferences(EntityManager entityManager, String sourceName, String query) {
        this.entityManager = entityManager;
        this.sourceName    = sourceName;
        this.query         = query;
    }

    @Override
    public String sourceName() {
        return sourceName;
    }

    @Override
    public Stream<String> referencedIdentifiers() {
        return entityManager.createQuery(query, String.class).getResultStream();
    }
}
