package org.jmouse.vault.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.vault.audit.VaultAudit;
import org.jmouse.vault.audit.VaultAuditEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 📜 The audit trail in {@code vault_audit_events}. Joins the caller's transaction. */
public final class JpaVaultAudit implements VaultAudit {

    private final EntityManager entityManager;

    public JpaVaultAudit(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entity manager");
    }

    @Override
    public void record(VaultAuditEvent event) {
        entityManager.persist(new VaultAuditRecord(UUID.randomUUID().toString(),
                UtcTime.of(event.occurredAt()),
                event.actor(), event.action().name(), event.subject(), event.source()));
    }

    @Override
    public List<VaultAuditEvent> recent(int limit) {
        return entityManager
                .createQuery("SELECT event FROM VaultAuditRecord event ORDER BY event.occurredAt DESC", VaultAuditRecord.class)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(record -> new VaultAuditEvent(UtcTime.toInstant(record.getOccurredAt()), record.getActor(),
                        VaultAuditEvent.Action.valueOf(record.getAction()), record.getSubject(), record.getSource()))
                .toList();
    }
}
