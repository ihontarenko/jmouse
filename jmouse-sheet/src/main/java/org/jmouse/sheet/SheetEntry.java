package org.jmouse.datamap;

import java.util.List;
import java.util.Optional;

/**
 * A block of a jMD document: {@code supply-voltage "Supply voltage" source page 7 { … }}. Its id is unique in its
 * document, however deep it is nested, so a link names it alone.
 *
 * @param title    the words it is shown with; empty for none
 * @param fields   its lines, in the order written
 * @param entries  the blocks inside it
 * @param comments what the author wrote above it and beside its opening line
 */
public record DataMapEntry(String id, Optional<String> title, Optional<DataMapSource> source,
                           List<DataMapField> fields, List<DataMapEntry> entries, DataMapComments comments) {

    public DataMapEntry {
        fields = List.copyOf(fields);
        entries = List.copyOf(entries);
    }

    public DataMapEntry(String id, Optional<String> title, Optional<DataMapSource> source,
                        List<DataMapField> fields, List<DataMapEntry> entries) {
        this(id, title, source, fields, entries, DataMapComments.NONE);
    }

    public Optional<DataMapField> field(String name) {
        return fields.stream().filter(field -> field.name().equals(name)).findFirst();
    }

    /** A field's value, when the entry has that field. */
    public Optional<DataMapValue> value(String name) {
        return field(name).map(DataMapField::value);
    }
}
