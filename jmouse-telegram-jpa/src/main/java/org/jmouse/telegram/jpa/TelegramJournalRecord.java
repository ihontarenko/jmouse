package org.jmouse.telegram.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jmouse.telegram.journal.JournalEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of the journal — a {@link JournalEntry}, persisted.
 *
 * <p>⚠️ Write-once. Nothing updates a journal row; {@link JpaTelegramJournal#prune} is the only thing that
 * removes one.
 *
 * <p>⚠️ No message text, for the reason {@link JournalEntry} gives.
 */
@Entity
@Table(name = TelegramJournalRecord.TABLE_NAME)
public class TelegramJournalRecord {

    /** Named once, so migrations and queries spell it the same way. */
    public static final String TABLE_NAME = "telegram_journal";

    /** Telegram's own refusal sentence can be long; the column is not a place to store an essay. */
    static final int DETAIL_LIMIT = 1000;

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "identity_name", length = 128)
    private String identityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 32)
    private JournalEntry.Operation operation;

    /** The chat's wire value: a signed id as text, or {@code @username}. */
    @Column(name = "chat", length = 64)
    private String chat;

    @Column(name = "thread_id")
    private Integer threadId;

    @Column(name = "message_id")
    private Integer messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 16)
    private JournalEntry.Outcome outcome;

    @Column(name = "refusal", length = 64)
    private String refusal;

    @Column(name = "detail", length = DETAIL_LIMIT)
    private String detail;

    @Column(name = "elapsed_milliseconds", nullable = false)
    private long elapsedMilliseconds;

    protected TelegramJournalRecord() {
        // Jakarta Persistence.
    }

    static TelegramJournalRecord of(JournalEntry entry) {
        TelegramJournalRecord record = new TelegramJournalRecord();

        record.id                  = UUID.randomUUID().toString();
        record.recordedAt          = entry.at();
        record.identityName        = entry.identityName();
        record.operation           = entry.operation();
        record.chat                = entry.chat();
        record.threadId            = entry.threadId();
        record.messageId           = entry.messageId();
        record.outcome             = entry.outcome();
        record.refusal             = entry.refusal();
        record.detail              = clipped(entry.detail());
        record.elapsedMilliseconds = entry.elapsed().toMillis();

        return record;
    }

    JournalEntry toEntry() {
        return new JournalEntry(recordedAt, identityName, operation, chat, threadId, messageId, outcome,
                                refusal, detail, Duration.ofMillis(elapsedMilliseconds));
    }

    private static String clipped(String value) {
        if (value == null || value.length() <= DETAIL_LIMIT) {
            return value;
        }

        return value.substring(0, DETAIL_LIMIT - 1) + "…";
    }
}
