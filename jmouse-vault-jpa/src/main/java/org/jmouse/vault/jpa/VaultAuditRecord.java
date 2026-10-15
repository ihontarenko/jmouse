package org.jmouse.vault.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 🗃️ One audit row. ⚠️ No value column exists, and none is to be added. */
@Entity
@Table(name = "vault_audit_events")
public class VaultAuditRecord {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "actor", length = 255)
    private String actor;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "source", length = 32)
    private String source;

    protected VaultAuditRecord() {
        // Jakarta Persistence.
    }

    VaultAuditRecord(String id, LocalDateTime occurredAt, String actor, String action, String subject, String source) {
        this.id         = id;
        this.occurredAt = occurredAt;
        this.actor      = actor;
        this.action     = action;
        this.subject    = subject;
        this.source     = source;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getSubject() {
        return subject;
    }

    public String getSource() {
        return source;
    }
}
