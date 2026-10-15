package org.jmouse.telegram.journal;

import java.util.List;

/**
 * One page of the journal, with the total a table needs to draw its pager.
 *
 * @param entries newest first
 * @param total   how many entries match the query in all, not only on this page
 */
public record JournalPage(List<JournalEntry> entries, long total) {

    public JournalPage {
        entries = List.copyOf(entries);
    }
}
