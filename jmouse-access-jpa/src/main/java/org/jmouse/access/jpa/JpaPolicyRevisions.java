package org.jmouse.access.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.access.jpa.entity.AccessPolicyRevision;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The policy history, against the engine's own table.
 *
 * <p>Plain JPA and JPQL, for the reason every store in this module gives: {@code jakarta.persistence}
 * and nothing else, so adopting the engine's storage does not also mean adopting a repository
 * framework.
 */
public class JpaPolicyRevisions implements PolicyRevisions {

    private final EntityManager    entityManager;
    private final Supplier<String> identifiers;

    public JpaPolicyRevisions(EntityManager entityManager) {
        this(entityManager, () -> UUID.randomUUID().toString());
    }

    public JpaPolicyRevisions(EntityManager entityManager, Supplier<String> identifiers) {
        this.entityManager = entityManager;
        this.identifiers   = identifiers;
    }

    @Override
    public Optional<Revision> inForce() {
        return entityManager.createQuery("""
                        SELECT revision FROM AccessPolicyRevision revision
                         ORDER BY revision.version DESC
                        """, AccessPolicyRevision.class)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .map(JpaPolicyRevisions::describe);
    }

    @Override
    public List<Revision> history() {
        return entityManager.createQuery("""
                        SELECT revision FROM AccessPolicyRevision revision
                         ORDER BY revision.version DESC
                        """, AccessPolicyRevision.class)
                .getResultList().stream()
                .map(JpaPolicyRevisions::describe)
                .toList();
    }

    @Override
    public Optional<Revision> byVersion(int version) {
        return entityManager.createQuery("""
                        SELECT revision FROM AccessPolicyRevision revision
                         WHERE revision.version = :version
                        """, AccessPolicyRevision.class)
                .setParameter("version", version)
                .getResultStream()
                .findFirst()
                .map(JpaPolicyRevisions::describe);
    }

    @Override
    public Revision save(String source, String note, String authorId, String authorLabel,
                         Integer revertedFrom, int roles, int subjects) {

        AccessPolicyRevision revision = new AccessPolicyRevision(
                identifiers.get(), nextVersion(), source, note,
                authorId, authorLabel, revertedFrom, roles, subjects);

        entityManager.persist(revision);

        return describe(revision);
    }

    /**
     * ⚠️ The maximum plus one, and the unique constraint is what makes it safe.
     *
     * <p>Two saves racing both read the same maximum and one of them fails on the key rather than
     * quietly overwriting the other's version number — which is the outcome to want, because a history
     * with two version 14s is a history nobody can revert through.
     */
    private int nextVersion() {
        Integer highest = entityManager.createQuery(
                        "SELECT MAX(revision.version) FROM AccessPolicyRevision revision", Integer.class)
                .getSingleResult();

        return highest == null ? 1 : highest + 1;
    }

    private static Revision describe(AccessPolicyRevision revision) {
        return new Revision(
                revision.getVersion(),
                revision.getSource(),
                revision.getNote(),
                revision.getAuthorId(),
                revision.getAuthorLabel(),
                revision.getRevertedFrom(),
                revision.getRoles(),
                revision.getSubjects(),
                localise(revision.getCreatedAt()));
    }

    private static LocalDateTime localise(Instant moment) {
        return moment == null ? null : moment.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
