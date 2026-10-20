package org.jmouse.parallax.jpa.catalogue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jmouse.parallax.jpa.NativeValues;
import org.jmouse.parallax.jpa.StoredPage;
import org.jmouse.parallax.object.ObjectKind;
import org.jmouse.parallax.owner.ParallaxOwner;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * 🧮 The objects: their index rows ({@link MathematicalObject}) and the truth behind them, every revision of their jMX
 * text in {@code object_sources}.
 *
 * <h3>⚠️ The texts are written by a native statement, and that orders the writes</h3>
 *
 * <p>{@code object_sources} has no entity: a revision is appended once and never read back as an object. An index row
 * persisted in the same transaction may still sit in the persistence context when its first revision is appended,
 * and the foreign key would then find nothing — so {@link #appendSource} flushes first, every time.</p>
 *
 * <p>⚠️ Every query reads a list, never a stream: outside a transaction the shared proxy closes its
 * {@code EntityManager} as the call returns, and a stream read after that finds its result set closed.</p>
 *
 * <p>It does not authorize anything, and transaction demarcation is the caller's throughout.</p>
 */
public class ObjectStore {

    private static final String BROWSE_CONDITIONS = """
            where object.ownerType is null
              and object.status in :statuses
              and (:category is null or object.categoryCode = :category)
              and (:kind is null or object.kind = :kind)
              and object.kind in :kinds
              and (:featured is null or object.featured = :featured)
              and (:text is null or lower(object.title) like :text or lower(object.summary) like :text
                   or lower(object.slug) like :text)
              and (:tag is null or :tag member of object.tags)
            """;

    private final EntityManager entityManager;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     */
    public ObjectStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 🔐 The checksum a text is cached and compared under: its SHA-256, in lowercase hex.
     *
     * @param source the jMX text
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
     * 🔎 One object by its identifier.
     *
     * @param id the identifier
     * @return the object, or empty
     */
    public Optional<MathematicalObject> find(String id) {
        return Optional.ofNullable(entityManager.find(MathematicalObject.class, id));
    }

    /**
     * 🔎 One object by its slug.
     *
     * @param slug the slug
     * @return the object, or empty
     */
    public Optional<MathematicalObject> findBySlug(String slug) {
        return entityManager.createQuery(
                        "select object from MathematicalObject object where object.slug = :slug", MathematicalObject.class)
                .setParameter("slug", slug)
                .getResultList()
                .stream()
                .findFirst();
    }

    /**
     * ❓ Whether a slug is taken.
     *
     * @param slug the slug
     * @return whether an object has it
     */
    public boolean existsBySlug(String slug) {
        return entityManager.createQuery(
                        "select count(object) from MathematicalObject object where object.slug = :slug", Long.class)
                .setParameter("slug", slug)
                .getSingleResult() > 0;
    }

    /**
     * 🔎 The objects with these identifiers, in no particular order; an identifier nothing has is left out.
     *
     * @param ids the identifiers
     * @return the objects found
     */
    public List<MathematicalObject> findByIds(Collection<String> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return entityManager.createQuery(
                        "select object from MathematicalObject object where object.id in :ids", MathematicalObject.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    /**
     * 🔎 The objects with these slugs, in no particular order; a slug nothing has is left out.
     *
     * @param slugs the slugs
     * @return the objects found
     */
    public List<MathematicalObject> findBySlugs(Collection<String> slugs) {
        if (slugs.isEmpty()) {
            return List.of();
        }

        return entityManager.createQuery(
                        "select object from MathematicalObject object where object.slug in :slugs", MathematicalObject.class)
                .setParameter("slugs", slugs)
                .getResultList();
    }

    /**
     * 🔎 Every object of one kind, catalogue and owned alike.
     *
     * @param kind the kind
     * @return the objects
     */
    public List<MathematicalObject> findByKind(ObjectKind kind) {
        return entityManager.createQuery(
                        "select object from MathematicalObject object where object.kind = :kind", MathematicalObject.class)
                .setParameter("kind", kind)
                .getResultList();
    }

    /**
     * 📚 Every object, catalogue and owned alike.
     *
     * @return the objects
     */
    public List<MathematicalObject> all() {
        return entityManager.createQuery("select object from MathematicalObject object", MathematicalObject.class)
                .getResultList();
    }

    /**
     * 📖 The catalogue listing: catalogue objects in the statuses asked for, narrowed by every filter that is not
     * {@code null}, featured first and then by title.
     *
     * @param statuses the statuses listed
     * @param category a category code, or {@code null} for any
     * @param kind     one kind, or {@code null} for any
     * @param kinds    the kinds listed at all (a nature's)
     * @param featured only featured or only not, or {@code null} for both
     * @param text     a lowercase {@code like} pattern over title, summary and slug, or {@code null}
     * @param tag      a tag the object must carry, or {@code null}
     * @param page     the page number, from zero
     * @param size     the page size
     * @return the page
     */
    public StoredPage<MathematicalObject> browse(Collection<MathematicalObject.Status> statuses, String category,
                                                 ObjectKind kind, Collection<ObjectKind> kinds, Boolean featured,
                                                 String text, String tag, int page, int size) {
        TypedQuery<MathematicalObject> rows = entityManager.createQuery(
                "select object from MathematicalObject object " + BROWSE_CONDITIONS
                + " order by object.featured desc, object.title asc", MathematicalObject.class);
        TypedQuery<Long> count = entityManager.createQuery(
                "select count(object) from MathematicalObject object " + BROWSE_CONDITIONS, Long.class);

        for (TypedQuery<?> query : List.of(rows, count)) {
            query.setParameter("statuses", statuses)
                    .setParameter("category", category)
                    .setParameter("kind", kind)
                    .setParameter("kinds", kinds)
                    .setParameter("featured", featured)
                    .setParameter("text", text)
                    .setParameter("tag", tag);
        }

        return StoredPage.of(rows, count, page, size);
    }

    /**
     * 📂 One owner's objects, most recently changed first.
     *
     * @param owner the owner
     * @param page  the page number, from zero
     * @param size  the page size
     * @return the page
     */
    public StoredPage<MathematicalObject> owned(ParallaxOwner owner, int page, int size) {
        String conditions = " where object.ownerType = :ownerType and object.ownerId = :ownerId";
        TypedQuery<MathematicalObject> rows = entityManager.createQuery(
                "select object from MathematicalObject object" + conditions + " order by object.updatedAt desc",
                MathematicalObject.class);
        TypedQuery<Long> count = entityManager.createQuery(
                "select count(object) from MathematicalObject object" + conditions, Long.class);

        for (TypedQuery<?> query : List.of(rows, count)) {
            query.setParameter("ownerType", owner.type()).setParameter("ownerId", owner.id());
        }

        return StoredPage.of(rows, count, page, size);
    }

    /**
     * 📋 Every object of one kind an owner holds, by title — a project's sheets.
     *
     * @param owner the owner
     * @param kind  the kind
     * @return the objects
     */
    public List<MathematicalObject> owned(ParallaxOwner owner, ObjectKind kind) {
        return entityManager.createQuery("select object from MathematicalObject object where object.ownerType = :ownerType"
                        + " and object.ownerId = :ownerId and object.kind = :kind order by object.title", MathematicalObject.class)
                .setParameter("ownerType", owner.type()).setParameter("ownerId", owner.id()).setParameter("kind", kind)
                .getResultList();
    }

    /**
     * ❓ Whether any object, catalogue or owned, is filed in a category.
     *
     * @param categoryCode the category
     * @return whether one is
     */
    public boolean anyInCategory(String categoryCode) {
        return entityManager.createQuery(
                        "select count(object) from MathematicalObject object where object.categoryCode = :category",
                        Long.class)
                .setParameter("category", categoryCode)
                .getSingleResult() > 0;
    }

    /**
     * 🔢 How many objects the shared catalogue holds, or how many belong to an owner, every status counted.
     *
     * @param owned true for the objects that belong to an owner, false for the shared catalogue's
     * @return the count
     */
    public long countByOwnership(boolean owned) {
        String condition = owned ? "object.ownerType is not null" : "object.ownerType is null";

        return entityManager.createQuery("select count(object) from MathematicalObject object where " + condition,
                                         Long.class)
                .getSingleResult();
    }

    /**
     * 🔢 How many catalogue objects of one status and of the given kinds a category holds; owned objects are not
     * counted.
     *
     * @param categoryCode the category
     * @param status       the status
     * @param kinds        the kinds counted — every kind for the whole section, one nature's for a tab of it
     * @return the count
     */
    public long countCatalogueInCategory(String categoryCode, MathematicalObject.Status status, List<ObjectKind> kinds) {
        return entityManager.createQuery("""
                        select count(object) from MathematicalObject object
                        where object.categoryCode = :category and object.ownerType is null and object.status = :status
                          and object.kind in :kinds
                        """, Long.class)
                .setParameter("category", categoryCode)
                .setParameter("status", status)
                .setParameter("kinds", kinds)
                .getSingleResult();
    }

    /**
     * 💾 Keep an object: a new one is persisted, a loaded one is already tracked.
     *
     * @param object the object
     * @return the same object, now managed
     */
    public MathematicalObject save(MathematicalObject object) {
        if (!entityManager.contains(object)) {
            entityManager.persist(object);
        }

        return object;
    }

    /**
     * 🚿 Write what the persistence context holds now, so a version read afterwards is the stored one.
     */
    public void flush() {
        entityManager.flush();
    }

    /**
     * 🗑️ Remove an object; its texts, tags and relations go with it (the schema cascades).
     *
     * @param object the object
     */
    public void remove(MathematicalObject object) {
        entityManager.remove(object);
    }

    // ── Texts ────────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 📜 The text of one revision, if the object has it.
     *
     * @param objectId the object
     * @param revision the revision
     * @return the text, or empty
     */
    public Optional<String> sourceAt(String objectId, int revision) {
        List<?> rows = entityManager.createNativeQuery(
                        "SELECT source FROM object_sources WHERE object_id = :objectId AND revision = :revision")
                .setParameter("objectId", objectId)
                .setParameter("revision", revision)
                .getResultList();

        return rows.stream().findFirst().map(NativeValues::stringOf);
    }

    /**
     * ✍️ Whether somebody saved the head revision — as opposed to the shipped catalogue writing its own file.
     *
     * @param objectId the object
     * @return whether the head was written by a person
     */
    public boolean isHeadEdited(String objectId) {
        Object count = entityManager.createNativeQuery("""
                        SELECT COUNT(*) FROM object_sources
                        WHERE object_id = :objectId AND written_by IS NOT NULL
                          AND revision = (SELECT MAX(revision) FROM object_sources WHERE object_id = :objectId)
                        """)
                .setParameter("objectId", objectId)
                .getSingleResult();

        return NativeValues.longOf(count) > 0;
    }

    /**
     * 📜 Every revision of an object, newest first, without their texts.
     *
     * @param objectId the object
     * @return the revisions
     */
    public List<ObjectSourceRevision> revisions(String objectId) {
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT revision, checksum, written_at, written_by FROM object_sources
                        WHERE object_id = :objectId ORDER BY revision DESC
                        """)
                .setParameter("objectId", objectId)
                .getResultList();

        return rows.stream()
                .map(Object[].class::cast)
                .map(columns -> new ObjectSourceRevision(NativeValues.intOf(columns[0]),
                                                         NativeValues.stringOf(columns[1]),
                                                         NativeValues.instantOf(columns[2]),
                                                         NativeValues.stringOf(columns[3])))
                .toList();
    }

    /**
     * ➕ Keep a text as the given revision of an object.
     *
     * <p>⚠️ Flushes first: the object row may have been persisted in this transaction and not written yet.</p>
     *
     * @param objectId  the object
     * @param revision  the revision it becomes
     * @param source    the jMX text
     * @param writtenBy who saved it; {@code null} for the shipped catalogue
     * @param writtenAt when
     */
    public void appendSource(String objectId, int revision, String source, String writtenBy, Instant writtenAt) {
        entityManager.flush();
        entityManager.createNativeQuery("""
                        INSERT INTO object_sources (object_id, revision, source, checksum, written_at, written_by)
                        VALUES (:objectId, :revision, :source, :checksum, :writtenAt, :writtenBy)
                        """)
                .setParameter("objectId", objectId)
                .setParameter("revision", revision)
                .setParameter("source", source)
                .setParameter("checksum", checksumOf(source))
                .setParameter("writtenAt", writtenAt)
                .setParameter("writtenBy", writtenBy)
                .executeUpdate();
    }
}
