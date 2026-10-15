package org.jmouse.telegram.journal;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * The most recent journal entries, in this process's memory.
 *
 * <p>⚠️ Bounded: beyond {@code capacity} the oldest entry goes. A restart forgets everything — enough to
 * answer "why did the message a minute ago not arrive", not "what happened last week".
 */
public final class MemoryTelegramJournal implements TelegramJournal {

    /** What is kept when nothing says otherwise. */
    public static final int DEFAULT_CAPACITY = 1000;

    private final int               capacity;
    private final Deque<JournalEntry> entries = new ArrayDeque<>();

    public MemoryTelegramJournal() {
        this(DEFAULT_CAPACITY);
    }

    public MemoryTelegramJournal(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("A journal has to keep at least one entry");
        }

        this.capacity = capacity;
    }

    @Override
    public synchronized void record(JournalEntry entry) {
        entries.addFirst(entry);

        while (entries.size() > capacity) {
            entries.removeLast();
        }
    }

    @Override
    public synchronized JournalPage list(JournalQuery query) {
        List<JournalEntry> matching = entries.stream().filter(query::matches).toList();
        int                from     = Math.min(query.offset(), matching.size());
        int                to       = Math.min(from + query.limit(), matching.size());

        return new JournalPage(matching.subList(from, to), matching.size());
    }

    @Override
    public synchronized long prune(Instant before) {
        long                   forgotten = 0;
        Iterator<JournalEntry> iterator  = entries.iterator();

        while (iterator.hasNext()) {
            if (iterator.next().at().isBefore(before)) {
                iterator.remove();
                forgotten++;
            }
        }

        return forgotten;
    }
}
