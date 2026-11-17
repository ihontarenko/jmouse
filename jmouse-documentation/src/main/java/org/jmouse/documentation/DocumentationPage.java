package org.jmouse.documentation;

import java.util.List;
import java.util.Objects;

/**
 * 📖 One page of an application's documentation: the subject it is about — a screen, a tool, a language, by the word the application
 * asks for it with —, its title and a line of what it says, its sections in order, and its text in the format a reader renders it from.
 *
 * @param subject  the word the page is asked for by: {@code pcb-editor}, {@code simulator}
 * @param title    what the page is called
 * @param summary  what it says, in a line; empty for none
 * @param format   the format its text is written in, as a reader renders it: {@code markdown}
 * @param text     the page itself, without what only says what it is
 * @param sections its sections, in order
 */
public record DocumentationPage(String subject, String title, String summary, String format, String text, List<Section> sections) {

    public DocumentationPage {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(title, "title");
        summary = summary == null ? "" : summary;
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(text, "text");
        sections = List.copyOf(sections);
        if (subject.isBlank()) {
            throw new IllegalArgumentException("A page of documentation is about something: its subject is empty");
        }
    }

    /**
     * A section of a page: its title, and the anchor a reader goes to it by.
     *
     * @param level how deep it stands: 2 for a page's own sections, 3 for one inside them
     */
    public record Section(String title, String anchor, int level) {
    }
}
