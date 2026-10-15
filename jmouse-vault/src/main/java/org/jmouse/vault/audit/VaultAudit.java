package org.jmouse.vault.audit;

import java.util.List;

/**
 * 📜 Where vault actions are written down. Who saw a secret is the question an incident starts with, so
 * every reveal goes here, and so does every change.
 */
public interface VaultAudit {

    void record(VaultAuditEvent event);

    /** Newest first. */
    List<VaultAuditEvent> recent(int limit);

    /** For an installation that keeps no record — say so on purpose, never by default. */
    static VaultAudit discarding() {
        return new VaultAudit() {
            @Override
            public void record(VaultAuditEvent event) {
            }

            @Override
            public List<VaultAuditEvent> recent(int limit) {
                return List.of();
            }
        };
    }
}
