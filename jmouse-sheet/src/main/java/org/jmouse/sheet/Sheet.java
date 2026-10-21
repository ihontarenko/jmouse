package org.jmouse.sheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A sheet document: a component's data, or any other table of facts, as entries holding fields.
 *
 * <pre>
 * sheet "74HC595 — 8-bit shift register" as 74hc595 {
 *     revision "SCLS041I"
 *
 *     supply-voltage "Supply voltage" source page 7 table 6.3 {
 *         range 2 V .. 6 V
 *     }
 * }
 * </pre>
 *
 * <p>An entry's id is unique in its document, however deep it is nested.</p>
 */
public final class Sheet {

    private final String slug;
    private final String title;
    private final List<SheetField> fields;
    private final List<SheetEntry> entries;
    private final SheetComments comments;
    private final Map<String, SheetEntry> index;

    /**
     * @param slug     the name it is known by
     * @param fields   the lines of the map itself
     * @param entries  its blocks, in the order written
     * @param comments what the author wrote above the document's first line
     * @throws IllegalArgumentException when two entries share an id
     */
    public Sheet(String slug, String title, List<SheetField> fields, List<SheetEntry> entries,
                   SheetComments comments) {
        this.slug = slug;
        this.title = title;
        this.fields = List.copyOf(fields);
        this.entries = List.copyOf(entries);
        this.comments = comments;
        this.index = indexOf(this.entries);
    }

    public Sheet(String slug, String title, List<SheetField> fields, List<SheetEntry> entries) {
        this(slug, title, fields, entries, SheetComments.NONE);
    }

    public SheetComments comments() {
        return comments;
    }

    public String slug() {
        return slug;
    }

    public String title() {
        return title;
    }

    public List<SheetField> fields() {
        return fields;
    }

    /** The blocks at the top of the map. */
    public List<SheetEntry> entries() {
        return entries;
    }

    /** The entry of that id, however deep it is nested. */
    public Optional<SheetEntry> entry(String id) {
        return Optional.ofNullable(index.get(id));
    }

    /** Every entry, nested ones included, in the order written. */
    public List<SheetEntry> allEntries() {
        return List.copyOf(index.values());
    }

    public Optional<SheetField> field(String name) {
        return fields.stream().filter(field -> field.name().equals(name)).findFirst();
    }

    private static Map<String, SheetEntry> indexOf(List<SheetEntry> entries) {
        Map<String, SheetEntry> index = new LinkedHashMap<>();
        List<SheetEntry> pending = new ArrayList<>(entries);
        while (!pending.isEmpty()) {
            SheetEntry entry = pending.removeFirst();
            if (index.putIfAbsent(entry.id(), entry) != null) {
                throw new IllegalArgumentException("two entries are called '" + entry.id() + "': an id names one entry");
            }
            pending.addAll(0, entry.entries());
        }
        return Collections.unmodifiableMap(index);
    }
}
