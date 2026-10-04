package org.jmouse.telegram.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 🗃️ One chat that can be reached on a subject's behalf.
 *
 * <p>See {@link TelegramBindings} for why this table is the precondition of every notification.
 *
 * <h2>⚠️ {@code chat_id} is a signed 64-bit number and is often negative</h2>
 *
 * <p>Groups are negative and supergroups are large negatives, conventionally prefixed {@code -100}. So
 * {@code BIGINT}, never {@code INT} and never unsigned — truncation delivers to a <em>different chat</em>
 * rather than failing, which is the worst available outcome for a notification.
 */
@Entity
@Table(name = TelegramBinding.TABLE_NAME)
public class TelegramBinding {

    /** Named once, so migrations and queries spell it the same way. */
    public static final String TABLE_NAME = "telegram_bindings";

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    /**
     * The product's own identifier for whatever is being reached.
     *
     * <p>⚠️ 128 and a plain string, because this library must not learn what a subject is — a person, a
     * workspace, a team, a channel. A UUID, a slug and a composite key all fit.
     */
    @Column(name = "subject", nullable = false, length = 128)
    private String subject;

    /** ⚠️ See the class note: BIGINT, signed. */
    @Column(name = "chat_id", nullable = false)
    private long chatId;

    /**
     * Who accepted the invitation.
     *
     * <p>⚠️ The id, not the username. A username is optional and can be changed or dropped, so a
     * binding keyed on one follows the wrong person after a rename.
     */
    @Column(name = "telegram_user_id", nullable = false)
    private long telegramUserId;

    /**
     * Their language, as Telegram reported it on the update that completed the binding.
     *
     * <p>⚠️ Captured here because there is no second chance: Telegram reports it on an update and
     * offers no endpoint to ask later. A binding without it means every notification to that person is
     * in whatever language the product guessed.
     */
    @Column(name = "language_code", length = 16)
    private String languageCode;

    /**
     * ⚠️ False once the bot was blocked, or the binding was switched off.
     *
     * <p>Kept rather than deleted so an administration screen can say "this person turned the bot off"
     * instead of showing nothing, which is indistinguishable from never having bound.
     */
    @Column(name = "deliverable", nullable = false)
    private boolean deliverable = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TelegramBinding() {
        // Jakarta Persistence.
    }

    public TelegramBinding(
            String id, String subject, long chatId, long telegramUserId, String languageCode) {

        this.id             = id;
        this.subject        = subject;
        this.chatId         = chatId;
        this.telegramUserId = telegramUserId;
        this.languageCode   = languageCode;
        this.createdAt      = LocalDateTime.now();
        this.updatedAt      = this.createdAt;
    }

    public String getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public long getChatId() {
        return chatId;
    }

    public long getTelegramUserId() {
        return telegramUserId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public boolean isDeliverable() {
        return deliverable;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** Re-accepting an invitation from the same chat revives a binding the bot had been blocked from. */
    public void revive(long telegramUserId, String languageCode) {
        this.telegramUserId = telegramUserId;
        this.languageCode   = languageCode;
        this.deliverable    = true;
        touch();
    }

    public void stopDelivering() {
        this.deliverable = false;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "TelegramBinding[subject=%s, chat=%d, deliverable=%s]"
                .formatted(subject, chatId, deliverable);
    }
}
