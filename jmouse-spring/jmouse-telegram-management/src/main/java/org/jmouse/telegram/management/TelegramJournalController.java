package org.jmouse.telegram.management;

import org.jmouse.telegram.journal.JournalEntry;
import org.jmouse.telegram.journal.JournalPage;
import org.jmouse.telegram.journal.JournalQuery;
import org.jmouse.telegram.journal.TelegramJournal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * The journal — every call to Telegram, newest first. The "why did nothing arrive" screen.
 *
 * <p>Read-only. Pruning is the retention thread's job, not a button: a journal an administrator can empty
 * is a journal that can be emptied right after the thing it would have shown.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX + "/journal")
public class TelegramJournalController {

    private final TelegramJournal journal;

    public TelegramJournalController(TelegramJournal journal) {
        this.journal = journal;
    }

    /**
     * @param identity only this identity's calls (the purpose's identity name)
     * @param chat     only this chat, by its wire value
     * @param outcome  {@code DELIVERED} or {@code REFUSED}
     * @param since    only entries at or after this instant, ISO-8601
     */
    @GetMapping
    public ManagementPage<JournalEntry> journal(
            @RequestParam(name = "identity", required = false) String identity,
            @RequestParam(name = "chat", required = false) String chat,
            @RequestParam(name = "outcome", required = false) JournalEntry.Outcome outcome,
            @RequestParam(name = "since", required = false) Instant since,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        int         bounded = ManagementRoutes.boundedLimit(limit);
        int         from    = Math.max(0, offset);
        JournalPage page    = journal.list(new JournalQuery(identity, chat, outcome, since, from, bounded));

        return new ManagementPage<>(page.entries(), page.total(), from, bounded);
    }
}
