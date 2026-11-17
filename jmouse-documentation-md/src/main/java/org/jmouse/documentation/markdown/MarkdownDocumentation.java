package org.jmouse.documentation.markdown;

import org.jmouse.core.io.Resource;
import org.jmouse.documentation.DocumentationPage;
import org.jmouse.documentation.DocumentationSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 📝 Pages of documentation written as Markdown files: what a page is in a front matter at its head — its {@code subject}, its
 * {@code title} and a {@code summary} line, between two {@code ---} lines —, the page itself after it, its sections its {@code ##} and
 * {@code ###} headings. The text is handed on as written; a reader renders it.
 *
 * <pre>
 * ---
 * subject: pcb-editor
 * title: The PCB editor
 * summary: Laying out a printed circuit board
 * ---
 * ## Tools
 * …
 * </pre>
 */
public final class MarkdownDocumentation implements DocumentationSource {

    /** The format a Markdown page's text is in, as a reader is told it. */
    public static final String FORMAT = "markdown";

    /** The line a front matter opens and closes with. */
    private static final String FENCE = "---";
    /** A front matter's line: a key, a colon, its value. */
    private static final Pattern FIELD = Pattern.compile("^([a-z][a-z-]*)\\s*:\\s*(.*)$");
    /** A heading a page's sections are told by: two or three hashes. */
    private static final Pattern HEADING = Pattern.compile("^(#{2,3})\\s+(.+?)\\s*#*\\s*$");

    private final List<DocumentationPage> pages;

    private MarkdownDocumentation(List<DocumentationPage> pages) {
        this.pages = List.copyOf(pages);
    }

    /** The pages the Markdown files given are, each read whole now. */
    public static MarkdownDocumentation of(Collection<? extends Resource> files) {
        List<DocumentationPage> pages = new ArrayList<>();
        for (Resource file : files) {
            try (InputStream stream = file.getInputStream()) {
                pages.add(pageOf(new String(stream.readAllBytes(), StandardCharsets.UTF_8), file.getName()));
            } catch (IOException unreadable) {
                throw new UncheckedIOException("The documentation file " + file.getName() + " cannot be read", unreadable);
            }
        }
        return new MarkdownDocumentation(pages);
    }

    @Override
    public List<DocumentationPage> pages() {
        return pages;
    }

    /** One Markdown text as a page; {@code name} is the file's, said where its front matter is wanting. */
    public static DocumentationPage pageOf(String markdown, String name) {
        List<String> lines = markdown.replace("\r\n", "\n").lines().toList();
        if (lines.isEmpty() || !lines.getFirst().strip().equals(FENCE)) {
            throw new IllegalArgumentException(name + " opens with no front matter: a page says its subject and title between two '---' lines");
        }
        Map<String, String> fields = new LinkedHashMap<>();
        int at = 1;
        while (at < lines.size() && !lines.get(at).strip().equals(FENCE)) {
            Matcher field = FIELD.matcher(lines.get(at).strip());
            if (field.matches()) {
                fields.put(field.group(1), field.group(2).strip());
            } else if (!lines.get(at).isBlank()) {
                throw new IllegalArgumentException(name + "'s front matter reads 'key: value' a line, not '" + lines.get(at).strip() + "'");
            }
            at++;
        }
        if (at == lines.size()) {
            throw new IllegalArgumentException(name + "'s front matter is not closed by a '---' line");
        }
        String subject = required(fields, "subject", name);
        String title = required(fields, "title", name);
        List<String> body = lines.subList(at + 1, lines.size());
        List<DocumentationPage.Section> sections = new ArrayList<>();
        boolean fenced = false;
        for (String line : body) {
            // A heading inside a code block is code, not a section.
            if (line.strip().startsWith("```")) {
                fenced = !fenced;
                continue;
            }
            Matcher heading = HEADING.matcher(line);
            if (!fenced && heading.matches()) {
                String text = heading.group(2);
                sections.add(new DocumentationPage.Section(text, anchorOf(text), heading.group(1).length()));
            }
        }
        return new DocumentationPage(subject, title, fields.getOrDefault("summary", ""), FORMAT, String.join("\n", body).strip(), sections);
    }

    private static String required(Map<String, String> fields, String key, String name) {
        String value = fields.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "'s front matter says no " + key);
        }
        return value;
    }

    /** The anchor a heading is gone to by, as Markdown renderers make it: its words in lower case, joined by hyphens. */
    public static String anchorOf(String heading) {
        return heading.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}\\s-]", "").strip().replaceAll("\\s+", "-");
    }
}
