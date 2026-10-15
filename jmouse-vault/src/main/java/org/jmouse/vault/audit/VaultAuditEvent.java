package org.jmouse.vault.audit;

import java.time.Instant;

/**
 * 📜 One thing somebody did to the vault. ⚠️ Never carries a value — not even a sealed one.
 *
 * @param subject the secret or credential name
 * @param source  the source acted on, or that answered a reveal
 */
public record VaultAuditEvent(Instant occurredAt, String actor, Action action, String subject, String source) {

    public enum Action {
        SECRET_SET,
        SECRET_DESCRIBED,
        SECRET_REMOVED,
        SECRET_REVEALED,
        CREDENTIAL_SAVED,
        CREDENTIAL_REMOVED,
        RESEALED
    }
}
