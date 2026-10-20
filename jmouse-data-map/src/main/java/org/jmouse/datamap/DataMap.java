package org.jmouse.datamap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A jMD document: a component's data, or any other table of facts, as entries holding fields.
 *
 * <pre>
 * map "74HC595 — 8-bit shift register" as 74hc595 {
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
public final class DataMap {

    private final String slug;
    private final String title;
    private final List<DataMapField> fields;
    private final List<DataMapEntry> entries;
    private final DataMapComments comments;
    private final Map<String, DataMapEntry> index;

    /**
     * @param slug     the name it is known by
     * @param fields   the lines of the map itself
     * @param entries  its blocks, in the order written
     * @param comments what the author wrote above the document's first line
     * @throws IllegalArgumentException when two entries share an id
     */
    public DataMap(String slug, String title, List<DataMapField> fields, List<DataMapEntry> entries,
                   DataMapComments comments) {
        this.slug = slug;
        this.title = title;
        this.fields = List.copyOf(fields);
        this.entries = List.copyOf(entries);
        this.comments = comments;
        this.index = indexOf(this.entries);
    }

    public DataMap(String slug, String title, List<DataMapField> fields, List<DataMapEntry> entries) {
        this(slug, title, fields, entries, DataMapComments.NONE);
    }

    public DataMapComments comments() {
        return comments;
    }

    public String slug() {
        return slug;
    }

    public String title() {
        return title;
    }

    public List<DataMapField> fields() {
        return fields;
    }

    /** The blocks at the top of the map. */
    public List<DataMapEntry> entries() {
        return entries;
    }

    /** The entry of that id, however deep it is nested. */
    public Optional<DataMapEntry> entry(String id) {
        return Optional.ofNullable(index.get(id));
    }

    /** Every entry, nested ones included, in the order written. */
    public List<DataMapEntry> allEntries() {
        return List.copyOf(index.values());
    }

    public Optional<DataMapField> field(String name) {
        return fields.stream().filter(field -> field.name().equals(name)).findFirst();
    }

    private static Map<String, DataMapEntry> indexOf(List<DataMapEntry> entries) {
        Map<String, DataMapEntry> index = new LinkedHashMap<>();
        List<DataMapEntry> pending = new ArrayList<>(entries);
        while (!pending.isEmpty()) {
            DataMapEntry entry = pending.removeFirst();
            if (index.putIfAbsent(entry.id(), entry) != null) {
                throw new IllegalArgumentException("two entries are called '" + entry.id() + "': an id names one entry");
            }
            pending.addAll(0, entry.entries());
        }
        return Collections.unmodifiableMap(index);
    }
}
