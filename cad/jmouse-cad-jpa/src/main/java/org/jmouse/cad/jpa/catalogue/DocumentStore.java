package org.jmouse.cad.jpa.catalogue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.jmouse.cad.jpa.NativeValues;
import org.jmouse.cad.jpa.StoredPage;
import org.jmouse.revisions.RevisionLabel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * 📚 The documents: their index rows ({@link StoredDocument}) and the truth behind them, every revision of their jM
 * text in {@code cad_document_revisions}.
 *
 * <h3>⚠️ The texts are written by a native statement, and that orders the writes</h3>
 *
 * <p>{@code cad_document_revisions} has no entity: a revision is appended once and never read back as a document. An
 * index row persisted in the same transaction may still sit in the persistence context when its first revision is
 * appended, and the foreign key would then find nothing — so {@link #appendSource} flushes first, every time.</p>
 *
 * <p>⚠️ Every query reads a list, never a stream: outside a transaction the shared proxy closes its
 * {@code EntityManager} as the call returns, and a stream read after that finds its result set closed.</p>
 *
 * <p>It does not authorize anything, and transaction demarcation is the caller's throughout.</p>
 */
public class DocumentStore {

    private static final String BROWSE_CONDITIONS = """
            where document.ownerType is null
              and document.status in :statuses
              and (:category is null or document.categoryCode = :category)
              and (:kind is null or document.kind = :kind)
              and (:featured is null or document.featured = :featured)
              and (:text is null or lower(document.title) like :text or lower(document.summary) like :text
                   or lower(document.slug) like :text)
              and (:tag is null or :tag member of document.tags)
            """;

    private final EntityManager entityManager;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     */
    public DocumentStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 🔐 The checksum a text is cached and compared under: its SHA-256, in lowercase hex.
     *
     * @param source the jM text
     * @return the checksum
     */
    public static String checksumOf(String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", impossible);
        }
    }

    // ── Index rows ───────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 🔎 One document by its identifier.
     *
     * @param id the identifier
     * @return the document, or empty
     */
    public Optional<StoredDocument> find(String id) {
        return Optional.ofNullable(entityManager.find(StoredDocument.class, id));
    }

    /**
     * 🔎 One document by its address: its identifier, or its slug — what a route names it by.
     *
     * @param address the identifier or the slug
     * @return the document, or empty
     */
    public Optional<StoredDocument> findByAddress(String address) {
        return find(address).or(() -> findBySlug(address));
    }

    /**
     * 🔎 One document by its slug.
     *
     * @param slug the slug
     * @return the document, or empty
     */
    public Optional<StoredDocument> findBySlug(String slug) {
        return entityManager.createQuery(
                        "select document from StoredDocument document where document.slug = :slug", StoredDocument.class)
                .setParameter("slug", slug)
                .getResultList()
                .stream()
                .findFirst();
    }

    /**
     * ❓ Whether a slug is taken.
     *
     * @param slug the slug
     * @return whether a document has it
     */
    public boolean existsBySlug(String slug) {
        return entityManager.createQuery(
                        "select count(document) from StoredDocument document where document.slug = :slug", Long.class)
                .setParameter("slug", slug)
                .getSingleResult() > 0;
    }

    /**
     * 🔎 The documents with these identifiers, in no particular order; an identifier nothing has is left out.
     *
     * @param ids the identifiers
     * @return the documents found
     */
    public List<StoredDocument> findByIds(Collection<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return entityManager.createQuery(
                        "select document from StoredDocument document where document.id in :ids", StoredDocument.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    /**
     * 🔎 The documents with these slugs, in no particular order; a slug nothing has is left out.
     *
     * @param slugs the slugs
     * @return the documents found
     */
    public List<StoredDocument> findBySlugs(Collection<String> slugs) {
        if (slugs.isEmpty()) {
            return List.of();
        }

        return entityManager.createQuery(
                        "select document from StoredDocument document where document.slug in :slugs", StoredDocument.class)
                .setParameter("slugs", slugs)
                .getResultList();
    }

    /**
     * The documents whose texts name this one — by a relation of that kind when one is given: the components landing on
     * a footprint.
     *
     * @param relatedDocumentId the document named
     * @param relationKind      the relation, as indexed ({@code footprint}), or {@code null} for any
     * @return the naming documents, each once
     */
    public List<StoredDocument> findRelating(String relatedDocumentId, String relationKind) {
        return entityManager.createQuery("""
                        select distinct document from StoredDocument document join document.relations relation
                        where relation.relatedDocumentId = :related and (:kind is null or relation.kind = :kind)""",
                                         StoredDocument.class)
                .setParameter("related", relatedDocumentId)
                .setParameter("kind", relationKind)
                .getResultList();
    }

    /**
     * Every document of one kind, catalogue and owned alike.
     *
     * @param kind the word its head opens with
     * @return the documents
     */
    public List<StoredDocument> findByKind(String kind) {
        return entityManager.createQuery(
                        "select document from StoredDocument document where document.kind = :kind", StoredDocument.class)
                .setParameter("kind", kind)
                .getResultList();
    }

    /**
     * 📖 The catalogue listing: catalogue documents in the statuses asked for, narrowed by every filter that is not
     * {@code null}, featured first and then by title.
     *
     * @param statuses the statuses listed
     * @param category a category code, or {@code null} for any
     * @param kind     one kind, or {@code null} for any
     * @param featured only featured or only not, or {@code null} for both
     * @param text     a lowercase {@code like} pattern over title, summary and slug, or {@code null}
     * @param tag      a tag the document must carry, or {@code null}
     * @param page     the page number, from zero
     * @param size     the page size
     * @return the page
     */
    public StoredPage<StoredDocument> browse(Collection<StoredDocument.Status> statuses, String category,
                                             String kind, Boolean featured, String text, String tag, int page,
                                             int size) {
        TypedQuery<StoredDocument> rows = entityManager.createQuery(
                "select document from StoredDocument document " + BROWSE_CONDITIONS
                + " order by document.featured desc, document.title asc", StoredDocument.class);
        TypedQuery<Long> count = entityManager.createQuery(
                "select count(document) from StoredDocument document " + BROWSE_CONDITIONS, Long.class);

        for (TypedQuery<?> query : List.of(rows, count)) {
            query.setParameter("statuses", statuses)
                    .setParameter("category", category)
                    .setParameter("kind", kind)
                    .setParameter("featured", featured)
                    .setParameter("text", text)
                    .setParameter("tag", tag);
        }

        return StoredPage.of(rows, count, page, size);
    }

    /**
     * 📂 One owner's documents, most recently changed first.
     *
     * @param owner the owner
     * @param page  the page number, from zero
     * @param size  the page size
     * @return the page
     */
    public StoredPage<StoredDocument> owned(DocumentOwner owner, int page, int size) {
        String conditions = " where document.ownerType = :ownerType and document.ownerId = :ownerId";
        TypedQuery<StoredDocument> rows = entityManager.createQuery(
                "select document from StoredDocument document" + conditions + " order by document.updatedAt desc",
                StoredDocument.class);
        TypedQuery<Long> count = entityManager.createQuery(
                "select count(document) from StoredDocument document" + conditions, Long.class);

        for (TypedQuery<?> query : List.of(rows, count)) {
            query.setParameter("ownerType", owner.type()).setParameter("ownerId", owner.id());
        }

        return StoredPage.of(rows, count, page, size);
    }

    /**
     * 📋 Every document of one kind an owner holds, by title — a project's sheets.
     *
     * @param owner the owner
     * @param kind  the kind
     * @return the documents
     */
    public List<StoredDocument> owned(DocumentOwner owner, String kind) {
        return entityManager.createQuery("select document from StoredDocument document where document.ownerType = :ownerType"
                        + " and document.ownerId = :ownerId and document.kind = :kind order by document.title", StoredDocument.class)
                .setParameter("ownerType", owner.type()).setParameter("ownerId", owner.id()).setParameter("kind", kind)
                .getResultList();
    }

    /**
     * ❓ Whether any document, catalogue or owned, is filed in a category.
     *
     * @param categoryCode the category
     * @return whether one is
     */
    public boolean anyInCategory(String categoryCode) {
        return entityManager.createQuery(
                        "select count(document) from StoredDocument document where document.categoryCode = :category",
                        Long.class)
                .setParameter("category", categoryCode)
                .getSingleResult() > 0;
    }

    /**
     * 🔢 How many catalogue documents of one status a category holds — of one kind when one is given; owned documents
     * are not counted.
     *
     * @param categoryCode the category
     * @param status       the status
     * @param kind         the kind counted, or {@code null} for every kind
     * @return the count
     */
    public long countCatalogueInCategory(String categoryCode, StoredDocument.Status status, String kind) {
        return entityManager.createQuery("""
                        select count(document) from StoredDocument document
                        where document.categoryCode = :category and document.ownerType is null and document.status = :status
                          and (:kind is null or document.kind = :kind)
                        """, Long.class)
                .setParameter("category", categoryCode)
                .setParameter("status", status)
                .setParameter("kind", kind)
                .getSingleResult();
    }

    /**
     * 💾 Keep a document: a new one is persisted, a loaded one is already tracked.
     *
     * @param document the document
     * @return the same document, now managed
     */
    public StoredDocument save(StoredDocument document) {
        if (!entityManager.contains(document)) {
            entityManager.persist(document);
        }

        return document;
    }

    /**
     * 🚿 Write what the persistence context holds now, so a version read afterwards is the stored one.
     */
    public void flush() {
        entityManager.flush();
    }

    /**
     * 🗑️ Remove a document; its texts, tags and relations go with it (the schema cascades).
     *
     * @param document the document
     */
    public void remove(StoredDocument document) {
        entityManager.remove(document);
    }

    // ── Texts ────────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 📜 The text of one revision, if the document has it.
     *
     * @param documentId the document
     * @param revision the revision
     * @return the text, or empty
     */
    public Optional<String> sourceAt(String documentId, int revision) {
        List<?> rows = entityManager.createNativeQuery(
                        "SELECT source FROM cad_document_revisions WHERE document_id = :documentId AND revision = :revision")
                .setParameter("documentId", documentId)
                .setParameter("revision", revision)
                .getResultList();

        return rows.stream().findFirst().map(NativeValues::stringOf);
    }

    /**
     * ✍️ Whether somebody saved the head revision — as opposed to the shipped catalogue writing its own file.
     *
     * @param documentId the document
     * @return whether the head was written by a person
     */
    public boolean isHeadEdited(String documentId) {
        Object count = entityManager.createNativeQuery("""
                        SELECT COUNT(*) FROM cad_document_revisions
                        WHERE document_id = :documentId AND written_by IS NOT NULL
                          AND revision = (SELECT MAX(revision) FROM cad_document_revisions WHERE document_id = :documentId)
                        """)
                .setParameter("documentId", documentId)
                .getSingleResult();

        return NativeValues.longOf(count) > 0;
    }

    /**
     * 📜 Every revision of a document, newest first, without their texts.
     *
     * @param documentId the document
     * @return the revisions
     */
    public List<DocumentRevision> revisions(String documentId) {
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT revision, revision_day, day_ordinal, checksum, written_at, written_by FROM cad_document_revisions
                        WHERE document_id = :documentId ORDER BY revision DESC
                        """)
                .setParameter("documentId", documentId)
                .getResultList();

        return rows.stream()
                .map(Object[].class::cast)
                .map(columns -> new DocumentRevision(NativeValues.intOf(columns[0]),
                                                         new RevisionLabel(NativeValues.dayOf(columns[1]),
                                                                           NativeValues.intOf(columns[2])),
                                                         NativeValues.stringOf(columns[3]),
                                                         NativeValues.instantOf(columns[4]),
                                                         NativeValues.stringOf(columns[5])))
                .toList();
    }

    /**
     * 🏷️ The label the next revision of a document written on {@code day} takes: that day, after every revision of it
     * already written that day. Two saves of one document at once collide on its revision number before they could share a
     * label, and the label is unique besides.
     *
     * @param documentId the document; one not stored yet has written nothing
     * @param day      the day, in the installation's time zone
     * @return the label
     */
    public RevisionLabel nextLabel(String documentId, LocalDate day) {
        Object written = entityManager.createNativeQuery("""
                        SELECT COUNT(*) FROM cad_document_revisions WHERE document_id = :documentId AND revision_day = :day
                        """)
                .setParameter("documentId", documentId)
                .setParameter("day", day)
                .getSingleResult();
        return RevisionLabel.next(day, (int) NativeValues.longOf(written));
    }

    /**
     * 🔎 The revision what follows an {@code @} names: a revision number ({@code lm358@3}), a label
     * ({@code lm358@20261023_001}), a day — its last revision — ({@code lm358@20261023}) or {@code latest}.
     *
     * @param documentId the document
     * @param written  what follows the {@code @}
     * @return the revision number, or empty when the text names none or no revision answers it
     */
    public Optional<Integer> revisionWritten(String documentId, String written) {
        String value = written == null ? "" : written.strip();
        if (RevisionLabel.Reference.written(value)) {
            return revisionOf(documentId, RevisionLabel.Reference.parse(value));
        }
        if (!value.matches("\\d{1,7}")) {
            return Optional.empty();
        }
        int revision = Integer.parseInt(value);
        return revision >= 1 ? Optional.of(revision) : Optional.empty();
    }

    /**
     * 🔎 The revision number a reference names: one revision by its label, the last of a day, or the latest.
     *
     * @param documentId  the document
     * @param reference what follows the {@code @}
     * @return the revision, or empty when none answers
     */
    public Optional<Integer> revisionOf(String documentId, RevisionLabel.Reference reference) {
        String condition = switch (reference) {
            case RevisionLabel.Exact ignored -> "AND revision_day = :day AND day_ordinal = :ordinal";
            case RevisionLabel.LastOfDay ignored -> "AND revision_day = :day";
            case RevisionLabel.Latest ignored -> "";
        };
        Query query = entityManager.createNativeQuery("SELECT MAX(revision) FROM cad_document_revisions WHERE document_id = :documentId "
                                                     + condition)
                .setParameter("documentId", documentId);
        switch (reference) {
            case RevisionLabel.Exact exact -> query.setParameter("day", exact.label().day())
                    .setParameter("ordinal", exact.label().ordinal());
            case RevisionLabel.LastOfDay lastOfDay -> query.setParameter("day", lastOfDay.day());
            case RevisionLabel.Latest ignored -> {
            }
        }
        Object found = query.getSingleResult();
        return found == null ? Optional.empty() : Optional.of(NativeValues.intOf(found));
    }

    /**
     * ➕ Keep a text as the given revision of a document, under the label it was given ({@link #nextLabel}).
     *
     * <p>⚠️ Flushes first: the document row may have been persisted in this transaction and not written yet.</p>
     *
     * @param documentId  the document
     * @param revision  the revision it becomes
     * @param label     the name it is said by
     * @param source    the jM text
     * @param writtenBy who saved it; {@code null} for the shipped catalogue
     * @param writtenAt when
     */
    public void appendSource(String documentId, int revision, RevisionLabel label, String source, String writtenBy,
                             Instant writtenAt) {
        entityManager.flush();
        entityManager.createNativeQuery("""
                        INSERT INTO cad_document_revisions (document_id, revision, source, checksum, written_at, written_by,
                                                    revision_day, day_ordinal)
                        VALUES (:documentId, :revision, :source, :checksum, :writtenAt, :writtenBy, :day, :ordinal)
                        """)
                .setParameter("documentId", documentId)
                .setParameter("revision", revision)
                .setParameter("source", source)
                .setParameter("checksum", checksumOf(source))
                .setParameter("writtenAt", writtenAt)
                .setParameter("writtenBy", writtenBy)
                .setParameter("day", label.day())
                .setParameter("ordinal", label.ordinal())
                .executeUpdate();
    }
}
