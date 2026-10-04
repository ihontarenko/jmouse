package org.jmouse.telegram.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 🗃️ An invitation waiting to be accepted.
 *
 * <h2>⚠️ This row grants somebody the right to receive a product's messages, so three rules are not
 * optional</h2>
 *
 * <ol>
 *   <li><strong>Single use.</strong> {@link #usedAt} is stamped on acceptance, and a stamped row is
 *       refused afterwards. A reusable link binds everybody who ever sees it.</li>
 *   <li><strong>It expires.</strong> A link that works forever is a link that works after it has been
 *       forwarded, screenshotted, or left in a chat log.</li>
 *   <li><strong>The token is opaque and unguessable.</strong> ⚠️ Never the subject in any encoding — a
 *       token derived from an identifier is a token anybody can compute for anybody.</li>
 * </ol>
 *
 * <p>Deliberately not deleted on use. A spent row is how "this invitation was already accepted, at this
 * time" is answerable, and it is what stops a re-sent link being quietly re-accepted.
 */
@Entity
@Table(name = TelegramBindingToken.TABLE_NAME)
public class TelegramBindingToken {

    /** Named once, so migrations and queries spell it the same way. */
    public static final String TABLE_NAME = "telegram_binding_tokens";

    /**
     * ⚠️ Telegram's own ceiling on a {@code /start} payload is 64 characters, so a token cannot be
     * longer however much entropy one might want. 32 base64url characters — 24 random bytes — sits well
     * inside it and is far beyond guessable.
     */
    public static final int TOKEN_LENGTH = 32;

    @Id
    @Column(name = "token", nullable = false, length = 64)
    private String token;

    @Column(name = "subject", nullable = false, length = 128)
    private String subject;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** ⚠️ Null until accepted; stamped exactly once. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected TelegramBindingToken() {
        // Jakarta Persistence.
    }

    public TelegramBindingToken(String token, String subject, LocalDateTime expiresAt) {
        this.token     = token;
        this.subject   = subject;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public String getToken() {
        return token;
    }

    public String getSubject() {
        return subject;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Whether this may still be accepted — unused and unexpired. */
    public boolean isOpen() {
        return usedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Stamps it as accepted.
     *
     * @throws IllegalStateException when it was already accepted. ⚠️ A guard rather than an overwrite:
     *                              silently re-stamping would let a replayed link bind a second chat
     */
    public void accept() {
        if (usedAt != null) {
            throw new IllegalStateException("this invitation was already accepted");
        }

        this.usedAt = LocalDateTime.now();
    }

    /** ⚠️ Never prints the token — a log line is a place an unused invitation must not appear. */
    @Override
    public String toString() {
        return "TelegramBindingToken[subject=%s, expiresAt=%s, used=%s]"
                .formatted(subject, expiresAt, usedAt != null);
    }
}
