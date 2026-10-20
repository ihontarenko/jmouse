package org.jmouse.datamap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * jMD documents by their paths — {@code parts/74hc595.jmd} —, and the links between them resolved. A link names its
 * document relative to the one that links: {@code 74hc595.jmd} beside it, {@code ../logic/74hc595.jmd} elsewhere.
 */
public final class DataMapLibrary {

    private static final String SEPARATOR = "/";
    private static final String CURRENT = ".";
    private static final String PARENT = "..";

    private final Map<String, DataMap> documents;

    private DataMapLibrary(Map<String, DataMap> documents) {
        this.documents = Collections.unmodifiableMap(documents);
    }

    /**
     * Reads every document.
     *
     * @param texts each document's text by its path, {@code /} separating directories
     */
    public static DataMapLibrary read(Map<String, String> texts) {
        Map<String, DataMap> documents = new LinkedHashMap<>();
        texts.forEach((path, text) -> documents.put(normalized(path), DataMapReader.read(path, text)));
        return new DataMapLibrary(documents);
    }

    public static DataMapLibrary of(Map<String, DataMap> documents) {
        Map<String, DataMap> byPath = new LinkedHashMap<>();
        documents.forEach((path, dataMap) -> byPath.put(normalized(path), dataMap));
        return new DataMapLibrary(byPath);
    }

    public Optional<DataMap> document(String path) {
        return Optional.ofNullable(documents.get(normalized(path)));
    }

    public Map<String, DataMap> documents() {
        return documents;
    }

    /** The entry a link names, the link read from the document at {@code from}. */
    public Optional<DataMapEntry> entry(String from, DataMapValue.Link link) {
        return document(resolve(from, link.document())).flatMap(dataMap -> dataMap.entry(link.id()));
    }

    /** The field a link names; empty for a link to an entry, or one that leads nowhere. */
    public Optional<DataMapField> field(String from, DataMapValue.Link link) {
        if (!link.toField()) {
            return Optional.empty();
        }
        return entry(from, link).flatMap(entry -> entry.field(link.field()));
    }

    /** Every link of every document that leads nowhere, as {@code path: link}. */
    public List<String> brokenLinks() {
        List<String> broken = new ArrayList<>();
        documents.forEach((path, dataMap) -> {
            List<DataMapValue.Link> links = new ArrayList<>();
            dataMap.fields().forEach(field -> collect(field.value(), links));
            dataMap.allEntries().forEach(entry -> entry.fields().forEach(field -> collect(field.value(), links)));
            for (DataMapValue.Link link : links) {
                boolean leads = link.toField() ? field(path, link).isPresent() : entry(path, link).isPresent();
                if (!leads) {
                    broken.add(path + ": " + link.written());
                }
            }
        });
        return broken;
    }

    private static void collect(DataMapValue value, List<DataMapValue.Link> links) {
        switch (value) {
            case DataMapValue.Link link -> links.add(link);
            case DataMapValue.Range range -> {
                collect(range.from(), links);
                collect(range.to(), links);
            }
            case DataMapValue.Assignment assignment -> collect(assignment.value(), links);
            case DataMapValue.Sequence sequence -> sequence.items().forEach(item -> collect(item, links));
            case DataMapValue.Text ignored -> {
            }
            case DataMapValue.Measure ignored -> {
            }
            case DataMapValue.Word ignored -> {
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
