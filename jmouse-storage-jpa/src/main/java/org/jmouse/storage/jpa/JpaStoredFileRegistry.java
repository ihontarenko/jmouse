package org.jmouse.storage.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jmouse.core.IdGenerator;
import org.jmouse.core.PrefixedIdGenerator;
import org.jmouse.storage.ContentTypes;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.StoredObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 📇 The registry, over Jakarta Persistence and nothing else.
 *
 * <p>No Spring Data repository, no {@code @Transactional}, no framework annotation of any kind:
 * the module has to work inside a Spring application, inside a jMouse one, and inside a plain
 * {@code EntityManager} test, and each of those demarcates transactions its own way. What this
 * class does is issue queries; deciding what they are part of stays outside it.</p>
 */
public class JpaStoredFileRegistry implements StoredFileRegistry {

    private static final String IDENTIFIER_PREFIX = "stored-file-";

    private static final String FIND_BY_STORAGE_KEY =
            "SELECT storedFile FROM StoredFile storedFile WHERE storedFile.storageKey = :storageKey";

    private static final String FIND_BY_SHA256 =
            "SELECT storedFile FROM StoredFile storedFile WHERE storedFile.sha256 = :sha256"
                    + " ORDER BY storedFile.createdAt ASC";

    private static final String LIST_IN_REGISTRATION_ORDER =
            "SELECT storedFile FROM StoredFile storedFile ORDER BY storedFile.createdAt ASC, storedFile.id ASC";

    private static final String LIST_REGISTERED_BEFORE =
            "SELECT storedFile FROM StoredFile storedFile WHERE storedFile.createdAt < :writtenBefore"
                    + " AND storedFile.id > :afterIdentifier ORDER BY storedFile.id ASC";

    private static final String COUNT_ALL = "SELECT COUNT(storedFile) FROM StoredFile storedFile";

    /**
     * 🔤 Sorts before every generated identifier, so "resume after nothing" needs no second query.
     */
    private static final String BEFORE_EVERY_IDENTIFIER = "";

    private final EntityManager               entityManager;
    private final IdGenerator<String, String> idGenerator;

    /**
     * 🏗️ Record writes into a persistence context, with generated identifiers.
     *
     * @param entityManager persistence context to work in
     */
    public JpaStoredFileRegistry(EntityManager entityManager) {
        this(entityManager, PrefixedIdGenerator.prefixedGenerator(IDENTIFIER_PREFIX));
    }

    /**
     * 🏗️ Record writes into a persistence context, with caller-chosen identifiers.
     *
     * @param entityManager persistence context to work in
     * @param idGenerator   produces registry identifiers
     */
    public JpaStoredFileRegistry(EntityManager entityManager, IdGenerator<String, String> idGenerator) {
        this.entityManager = entityManager;
        this.idGenerator   = idGenerator;
    }

    /**
     * ✅ Record an object, taking the backend from the write receipt itself.
     *
     * <p>Deliberately not from a store this registry was constructed with. An application may run
     * several backends, and a caller may name one per write, so the only source that cannot be
     * wrong is the receipt handed back by whichever store actually did the writing.</p>
     */
    @Override
    public StoredFile register(StoredObject object, String originalName) {
        StoredFile storedFile = new StoredFile(
                idGenerator.generate(),
                object.key().value(),
                originalName,
                ContentTypes.baseType(object.contentType()),
                object.sizeBytes(),
                object.sha256(),
                object.backendName(),
                LocalDateTime.now()
        );

        entityManager.persist(storedFile);

        return storedFile;
    }

    @Override
    public Optional<StoredFile> find(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(entityManager.find(StoredFile.class, identifier));
    }

    @Override
    public Optional<StoredFile> findByStorageKey(StorageKey key) {
        return firstOf(entityManager.createQuery(FIND_BY_STORAGE_KEY, StoredFile.class)
                               .setParameter("storageKey", key.value()));
    }

    @Override
    public Optional<StoredFile> findBySha256(String sha256) {
        if (sha256 == null || sha256.isBlank()) {
            return Optional.empty();
        }

        return firstOf(entityManager.createQuery(FIND_BY_SHA256, StoredFile.class)
                               .setParameter("sha256", sha256));
    }

    @Override
    public List<StoredFile> list(int offset, int limit) {
        return entityManager.createQuery(LIST_IN_REGISTRATION_ORDER, StoredFile.class)
                .setFirstResult(Math.max(offset, 0))
                .setMaxResults(Math.max(limit, 0))
                .getResultList();
    }

    @Override
    public List<StoredFile> listRegisteredBefore(LocalDateTime writtenBefore, String afterIdentifier,
                                                 int limit) {
        String cursor = (afterIdentifier == null) ? BEFORE_EVERY_IDENTIFIER : afterIdentifier;

        return entityManager.createQuery(LIST_REGISTERED_BEFORE, StoredFile.class)
                .setParameter("writtenBefore", writtenBefore)
                .setParameter("afterIdentifier", cursor)
                .setMaxResults(Math.max(limit, 0))
                .getResultList();
    }

    @Override
    public void remove(StoredFile storedFile) {
        StoredFile attached = entityManager.contains(storedFile)
                ? storedFile
                : entityManager.find(StoredFile.class, storedFile.getIdentifier());

        if (attached != null) {
            entityManager.remove(attached);
        }
    }

    @Override
    public long count() {
        return entityManager.createQuery(COUNT_ALL, Long.class).getSingleResult();
    }

    /**
     * 🥇 The first row a query returns, without a second query to ask whether there is one.
     *
     * @param query query to run, capped at one row
     * @return the row, or empty
     */
    private Optional<StoredFile> firstOf(TypedQuery<StoredFile> query) {
        return query.setMaxResults(1).getResultList().stream().findFirst();
    }
}
