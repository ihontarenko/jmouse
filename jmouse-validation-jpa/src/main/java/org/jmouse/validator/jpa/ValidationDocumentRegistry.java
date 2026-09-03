package org.jmouse.validator.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The documents, as rows. 🗄️
 *
 * <h2>⚠️ Jakarta Persistence only — no Spring, and no transaction demarcation</h2>
 *
 * <p>The same rule every self-migrating library here follows: transactions belong to whoever calls
 * this, not to the library. A store that opened its own would fight the caller's, and the day it wins
 * is the day a product's save is committed while the change that was supposed to go with it is rolled
 * back.</p>
 *
 * <h2>⚠️ It stores and reads. It does not parse.</h2>
 *
 * <p>Whether the text is a valid document is answered by {@code JmvReader}, and a store that checked
 * would be a second place where a document can be refused — with its own message, its own idea of what
 * a line number is, and its own opportunity to disagree. A caller that wants a document checked before
 * it is kept reads it first; this keeps what it is given.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ValidationDocumentRegistry {

    private final EntityManager entityManager;
    private final Clock         clock;

    public ValidationDocumentRegistry(EntityManager entityManager) {
        this(entityManager, Clock.systemUTC());
    }

    public ValidationDocumentRegistry(EntityManager entityManager, Clock clock) {
        this.entityManager = Objects.requireNonNull(entityManager, "A document registry needs an entity manager");
        this.clock = Objects.requireNonNull(clock, "A document registry needs a clock");
    }

    /**
     * One document by the name it calls itself.
     *
     * @param name the document's name — {@code innoventa/part}
     * @return it, or empty where nothing is stored under that name
     */
    public Optional<ValidationDocument> find(String name) {
        TypedQuery<ValidationDocument> query = entityManager.createQuery(
                "SELECT document FROM ValidationDocument document WHERE document.name = :name",
                ValidationDocument.class);

        query.setParameter("name", name);

        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException absent) {
            return Optional.empty();
        }
    }

    /**
     * One document by its identifier.
     *
     * <p>⚠️ Beside {@link #find(String)}, not instead of it, because the two answer different
     * questions. The <strong>name</strong> is how the language addresses a document and how a loader
     * resolves one written on disk. The <strong>id</strong> is how something that has already chosen a
     * document keeps hold of it — and it is the one that survives a rename, which a pointer stored in
     * another table needs it to.</p>
     *
     * @param id the document's identifier
     * @return it, or empty where nothing is stored under that id
     */
    public Optional<ValidationDocument> byId(String id) {
        return Optional.ofNullable(entityManager.find(ValidationDocument.class, id));
    }

    /**
     * Every document, by name.
     *
     * <p>⚠️ Ordered by name rather than by when it was written. A listing whose order changed with the
     * last edit is one nobody can scan twice.</p>
     *
     * @return the documents
     */
    public List<ValidationDocument> all() {
        return entityManager.createQuery(
                "SELECT document FROM ValidationDocument document ORDER BY document.name",
                ValidationDocument.class).getResultList();
    }

    /**
     * Writes a document, creating it or replacing what it says.
     *
     * <p>⚠️ Keyed on the name, which is what makes this idempotent for a caller that does not know
     * whether the document exists — and what stops two saves of one form producing two rows that
     * disagree.</p>
     *
     * @param name   what the document calls itself
     * @param source the {@code .jmv}, as written
     * @return the stored document
     */
    public ValidationDocument write(String name, String source) {
        Instant now = clock.instant();

        return find(name)
                .map(document -> {
                    document.rewrite(source, now);

                    return document;
                })
                .orElseGet(() -> {
                    ValidationDocument document = new ValidationDocument(name, source, now);

                    entityManager.persist(document);

                    return document;
                });
    }

    /**
     * Removes a document.
     *
     * <p>⚠️ Nothing here checks whether a product still points at it. This library does not know what
     * points at a document and must not guess — a product that keeps a reference enforces it with its
     * own foreign key, where the database can refuse rather than a library hoping.</p>
     *
     * @param name what the document calls itself
     * @return whether anything was removed
     */
    public boolean remove(String name) {
        return find(name)
                .map(document -> {
                    entityManager.remove(document);

                    return true;
                })
                .orElse(false);
    }
}
