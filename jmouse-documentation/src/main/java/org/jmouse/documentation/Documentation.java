package org.jmouse.documentation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🗂️ An application's documentation: every page its sources hold, found by its subject. Two pages of one subject are refused as they
 * are read — a reader asking for the pcb editor's page must find one page, never whichever was read last.
 */
public final class Documentation {

    private final Map<String, DocumentationPage> bySubject = new LinkedHashMap<>();

    public Documentation(List<? extends DocumentationSource> sources) {
        for (DocumentationSource source : sources) {
            for (DocumentationPage page : source.pages()) {
                DocumentationPage before = bySubject.putIfAbsent(page.subject(), page);
                if (before != null) {
                    throw new IllegalStateException("Two pages of documentation are about '" + page.subject() + "': '" + before.title()
                                                    + "' and '" + page.title() + "'");
                }
            }
        }
    }

    /** The page about a subject; empty where none is. */
    public Optional<DocumentationPage> page(String subject) {
        return Optional.ofNullable(bySubject.get(subject));
    }

    /** Every page, by its title. */
    public List<DocumentationPage> pages() {
        List<DocumentationPage> pages = new ArrayList<>(bySubject.values());
        pages.sort(Comparator.comparing(DocumentationPage::title, String.CASE_INSENSITIVE_ORDER));
        return pages;
    }
}
