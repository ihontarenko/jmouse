package org.jmouse.telegram.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jmouse.telegram.IdentityKind;

import java.time.LocalDateTime;

/**
 * 🗃️ One configured Telegram identity, as a row an administrator edits.
 *
 * <p>The alternative to this table is a properties file, which
 * {@code jmouse-telegram-spring-boot} already provides and which is the right answer for an
 * installation whose bots never change. This exists for the one that rotates a token without a deploy,
 * switches an account off during an incident, or adds a purpose from a screen.
 *
 * <h2>⚠️ The purpose is the key, not the name</h2>
 *
 * <p>{@link #purpose} carries the unique constraint because a purpose is what a caller asks for —
 * {@code gateway.as("kitsu-notifications")} — and two rows claiming one purpose would make which
 * identity answers depend on row order. `general` is the fallback every unconfigured purpose resolves
 * to, and it is a name rather than a null precisely so it is findable in a query and labellable on a
 * screen.
 *
 * <h2>⚠️ The credential is sealed, and this entity never unseals it</h2>
 *
 * <p>{@link #credentialSealed} holds whatever {@link CredentialCipher} produced. Opening it is
 * {@link JpaIdentitySource}'s business, at the moment of use, so that a read of the table — a
 * listing, an administration screen, a debug query — never materialises a token anywhere.
 *
 * <p>⚠️ There is no accessor returning the credential in the clear, and adding one would defeat the
 * arrangement: the whole point is that an entity loaded into a persistence context cannot leak a
 * token into a log line, a JSON response, or a `toString`.
 *
 * <h2>Mapping it in a product</h2>
 *
 * <p>This entity lives outside a product's own package, so the default scan does not reach it:</p>
 *
 * <pre>{@code
 * @EntityScan({"net.innoventa", "org.jmouse.telegram.jpa"})
 * }</pre>
 */
@Entity
@Table(name = TelegramAccount.TABLE_NAME)
public class TelegramAccount {

    /** Named once, so migrations, queries and any product foreign key spell it the same way. */
    public static final String TABLE_NAME = "telegram_accounts";

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    /**
     * What a caller asks for. Unique — see the class note.
     *
     * <p>64 because it is a product's own word and a short one; long enough for
     * {@code kitsu-notifications} several times over.
     */
    @Column(name = "purpose", nullable = false, length = 64, unique = true)
    private String purpose;

    /**
     * ⚠️ Stored as a STRING rather than an ordinal. An ordinal means inserting a constant into
     * {@link IdentityKind} silently re-points every existing row, and the symptom is a bot token being
     * handed to an MTProto transport.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private IdentityKind kind;

    /**
     * The sealed credential — see {@link CredentialCipher}.
     *
     * <p>1024 because AES-GCM with base64 inflates by roughly a third and a user session reference is
     * substantially longer than a bot token; generous rather than tight, since a truncated credential is
     * unrecoverable and looks exactly like a rotated key.
     */
    @Column(name = "credential_sealed", nullable = false, length = 1024)
    private String credentialSealed;

    /** A self-hosted {@code telegram-bot-api} server, or null for Telegram's own. */
    @Column(name = "api_base", length = 255)
    private String apiBase;

    /**
     * ⚠️ So an account can be switched off without deleting its configuration.
     *
     * <p>Deleting a row during an incident loses the token and the audit of it ever existing; disabling
     * is reversible by whoever is awake.
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TelegramAccount() {
        // Jakarta Persistence.
    }

    public TelegramAccount(
            String id, String purpose, IdentityKind kind, String credentialSealed, String apiBase) {

        this.id               = id;
        this.purpose          = purpose;
        this.kind             = kind;
        this.credentialSealed = credentialSealed;
        this.apiBase          = apiBase;
        this.createdAt        = LocalDateTime.now();
        this.updatedAt        = this.createdAt;
    }

    public String getId() {
        return id;
    }

    public String getPurpose() {
        return purpose;
    }

    public IdentityKind getKind() {
        return kind;
    }

    /** ⚠️ Sealed. Opening it is {@link JpaIdentitySource}'s job, at the moment of use. */
    public String getCredentialSealed() {
        return credentialSealed;
    }

    public String getApiBase() {
        return apiBase;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void reseal(String credentialSealed) {
        this.credentialSealed = credentialSealed;
        touch();
    }

    public void pointAt(String apiBase) {
        this.apiBase = apiBase;
        touch();
    }

    public void enable() {
        this.enabled = true;
        touch();
    }

    public void disable() {
        this.enabled = false;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /** ⚠️ Names the account and never its credential, sealed or otherwise. */
    @Override
    public String toString() {
        return "TelegramAccount[purpose=%s, kind=%s, enabled=%s]".formatted(purpose, kind, enabled);
    }
}
