package org.jmouse.telegram.journal;

import java.time.Instant;
import java.util.List;

/**
 * Where journal entries are kept.
 *
 * <p>{@link MemoryTelegramJournal} is here; a database-backed one is in {@code jmouse-telegram-jpa}.
 * {@link JournalingTransport} is what writes to it.
 *
 * <p>⚠️ <strong>Recording must not fail a send.</strong> The message has already gone by the time an
 * entry is written, so an implementation that throws is reporting a failure that did not happen.
 * {@link JournalingTransport} catches and logs whatever {@link #record} throws; an implementation should
 * still prefer swallowing its own trouble to raising it.
 */
public interface TelegramJournal {

    void record(JournalEntry entry);

    JournalPage list(JournalQuery query);

    /**
     * Forgets entries older than {@code before} — a journal is a diagnostic, not an archive.
     *
     * @return how many were forgotten
     */
    long prune(Instant before);

    /** A journal that keeps nothing — for an application that has decided it wants none. */
    static TelegramJournal none() {
        return new TelegramJournal() {
            @Override
            public void record(JournalEntry entry) {
            }

            @Override
            public JournalPage list(JournalQuery query) {
                return new JournalPage(List.of(), 0);
            }

            @Override
            public long prune(Instant before) {
                return 0;
            }
        };
    }
}
