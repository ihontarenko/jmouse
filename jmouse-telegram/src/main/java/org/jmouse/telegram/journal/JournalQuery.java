package org.jmouse.telegram.journal;

import java.time.Instant;

/**
 * Which journal entries to list — every filter optional, newest first.
 *
 * @param identityName only this identity's calls
 * @param chat         only this chat, by its wire value
 * @param outcome      only deliveries, or only refusals
 * @param since        only entries at or after this time
 * @param offset       how many matching entries to skip
 * @param limit        how many to return, at most {@link #MAXIMUM_LIMIT}
 */
public record JournalQuery(
        String               identityName,
        String               chat,
        JournalEntry.Outcome outcome,
        Instant              since,
        int                  offset,
        int                  limit
) {

    /** What a page holds when nothing says otherwise. */
    public static final int DEFAULT_LIMIT = 50;

    /** The most a page may hold — a listing is for reading, not for export. */
    public static final int MAXIMUM_LIMIT = 500;

    public JournalQuery {
        identityName = blankToNull(identityName);
        chat         = blankToNull(chat);
        offset       = Math.max(0, offset);
        limit        = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAXIMUM_LIMIT);
    }

    /** The most recent entries, unfiltered. */
    public static JournalQuery latest() {
        return new JournalQuery(null, null, null, null, 0, DEFAULT_LIMIT);
    }

    /** Only the refusals — the "why did nothing arrive" listing. */
    public static JournalQuery refusals() {
        return new JournalQuery(null, null, JournalEntry.Outcome.REFUSED, null, 0, DEFAULT_LIMIT);
    }

    /** Whether an entry passes every filter this query sets. */
    public boolean matches(JournalEntry entry) {
        if (identityName != null && !identityName.equals(entry.identityName())) {
            return false;
        }

        if (chat != null && !chat.equals(entry.chat())) {
            return false;
        }

        if (outcome != null && outcome != entry.outcome()) {
            return false;
        }

        return since == null || !entry.at().isBefore(since);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
