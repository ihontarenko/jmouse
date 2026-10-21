package org.jmouse.sheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * sheet documents by their paths — {@code parts/74hc595.sht} —, and the links between them resolved. A link names its
 * document relative to the one that links: {@code 74hc595.sht} beside it, {@code ../logic/74hc595.sht} elsewhere.
 */
public final class SheetLibrary {

    private static final String SEPARATOR = "/";
    private static final String CURRENT = ".";
    private static final String PARENT = "..";

    private final Map<String, Sheet> documents;

    private SheetLibrary(Map<String, Sheet> documents) {
        this.documents = Collections.unmodifiableMap(documents);
    }

    /**
     * Reads every document.
     *
     * @param texts each document's text by its path, {@code /} separating directories
     */
    public static SheetLibrary read(Map<String, String> texts) {
        Map<String, Sheet> documents = new LinkedHashMap<>();
        texts.forEach((path, text) -> documents.put(normalized(path), SheetReader.read(path, text)));
        return new SheetLibrary(documents);
    }

    public static SheetLibrary of(Map<String, Sheet> documents) {
        Map<String, Sheet> byPath = new LinkedHashMap<>();
        documents.forEach((path, sheet) -> byPath.put(normalized(path), sheet));
        return new SheetLibrary(byPath);
    }

    public Optional<Sheet> document(String path) {
        return Optional.ofNullable(documents.get(normalized(path)));
    }

    public Map<String, Sheet> documents() {
        return documents;
    }

    /** The entry a link names, the link read from the document at {@code from}. */
    public Optional<SheetEntry> entry(String from, SheetValue.Link link) {
        return document(resolve(from, link.document())).flatMap(sheet -> sheet.entry(link.id()));
    }

    /** The field a link names; empty for a link to an entry, or one that leads nowhere. */
    public Optional<SheetField> field(String from, SheetValue.Link link) {
        if (!link.toField()) {
            return Optional.empty();
        }
        return entry(from, link).flatMap(entry -> entry.field(link.field()));
    }

    /** Every link of every document that leads nowhere, as {@code path: link}. */
    public List<String> brokenLinks() {
        List<String> broken = new ArrayList<>();
        documents.forEach((path, sheet) -> {
            List<SheetValue.Link> links = new ArrayList<>();
            sheet.fields().forEach(field -> collect(field.value(), links));
            sheet.allEntries().forEach(entry -> entry.fields().forEach(field -> collect(field.value(), links)));
            for (SheetValue.Link link : links) {
                boolean leads = link.toField() ? field(path, link).isPresent() : entry(path, link).isPresent();
                if (!leads) {
                    broken.add(path + ": " + link.written());
                }
            }
        });
        return broken;
    }

    private static void collect(SheetValue value, List<SheetValue.Link> links) {
        switch (value) {
            case SheetValue.Link link -> links.add(link);
            case SheetValue.Range range -> {
                collect(range.from(), links);
                collect(range.to(), links);
            }
            case SheetValue.Assignment assignment -> collect(assignment.value(), links);
            case SheetValue.Sequence sequence -> sequence.items().forEach(item -> collect(item, links));
            case SheetValue.Text ignored -> {
            }
            case SheetValue.Measure ignored -> {
            }
            case SheetValue.Word ignored -> {
            }
        }
    }

    /** A document named relative to another, as a path in this library. */
    static String resolve(String from, String document) {
        String normalizedFrom = normalized(from);
        int slash = normalizedFrom.lastIndexOf(SEPARATOR);
        String directory = slash < 0 ? "" : normalizedFrom.substring(0, slash + 1);
        return normalized(directory + document);
    }

    /** A path with its {@code .} and {@code ..} walked and {@code \} read as {@code /}. */
    static String normalized(String path) {
        List<String> parts = new ArrayList<>();
        for (String part : path.replace('\\', '/').split(SEPARATOR)) {
            if (part.isEmpty() || part.equals(CURRENT)) {
                continue;
            }
            if (part.equals(PARENT) && !parts.isEmpty() && !parts.getLast().equals(PARENT)) {
                parts.removeLast();
            } else {
                parts.add(part);
            }
        }
        return String.join(SEPARATOR, parts);
    }
}
