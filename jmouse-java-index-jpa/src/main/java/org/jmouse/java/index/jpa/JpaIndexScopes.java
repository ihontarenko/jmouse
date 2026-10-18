package org.jmouse.java.index.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.util.Objects;
import java.util.UUID;

/** Optional database snapshot lifecycle. Caller owns the EntityManager and its transaction. */
public final class JpaIndexScopes {
    private final EntityManager entityManager;

    public JpaIndexScopes(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager);
    }

    public String create(IndexConfiguration configuration) {
        requireTransaction();
        IndexScopeRow row = new IndexScopeRow();
        row.id = UUID.randomUUID().toString();
        row.configuration = configuration.key();
        entityManager.persist(row);
        return row.id;
    }

    public void seal(String scopeId) {
        IndexScopeRow scope = locked(scopeId);
        if (!scope.sealed) {
            scope.sealed = true;
            scope.revision++;
        }
    }

    /** Copies only membership from a sealed snapshot; contributions and SQL facts remain shared. */
    public String copy(String sourceScopeId, IndexConfiguration configuration) {
        requireTransaction();
        IndexScopeRow source = entityManager.find(IndexScopeRow.class, sourceScopeId);
        if (source == null || !source.sealed || !source.configuration.equals(configuration.key())) {
            throw new IllegalArgumentException(
                    "Copy requires a sealed scope with matching configuration.");
        }
        String destination = create(configuration);
        var entries =
                entityManager
                        .createQuery(
                                "SELECT entry FROM ScopeEntryRow entry WHERE entry.scopeId ="
                                        + " :scope",
                                ScopeEntryRow.class)
                        .setParameter("scope", sourceScopeId)
                        .getResultList();
        for (ScopeEntryRow entry : entries) {
            ScopeEntryRow copy = new ScopeEntryRow();
            copy.id = IndexIdentities.hash(destination + ":" + entry.sourceHash);
            copy.scopeId = destination;
            copy.sourceHash = entry.sourceHash;
            copy.source = entry.source;
            copy.contributionId = entry.contributionId;
            entityManager.persist(copy);
        }
        return destination;
    }

    IndexScopeRow locked(String scopeId) {
        requireTransaction();
        entityManager.flush();
        IndexScopeRow scope =
                entityManager.find(IndexScopeRow.class, scopeId, LockModeType.PESSIMISTIC_WRITE);
        if (scope == null) {
            throw new IllegalArgumentException("Unknown Java index scope.");
        }
        entityManager.refresh(scope, LockModeType.PESSIMISTIC_WRITE);
        return scope;
    }

    void requireTransaction() {
        if (!entityManager.isJoinedToTransaction()) {
            throw new IllegalStateException(
                    "Java index mutation requires the caller's active transaction.");
        }
    }
}
