package org.jmouse.datamap;

import java.util.List;

/**
 * Writes a jMD document: four spaces to a level, a map's or an entry's fields first and its entries after them, a
 * blank line before each entry that follows something, and every comment back above or beside its line. What
 * {@link DataMapReader} reads back is the same map.
 */
public final class DataMapWriter {

    private static final String INDENT = "    ";

    private DataMapWriter() {
    }

    public static String write(DataMap dataMap) {
        StringBuilder text = new StringBuilder();
        dataMap.comments().above().forEach(comment -> text.append(comment).append('\n'));
        text.append("map ").append(DataMapValue.quoted(dataMap.title())).append(" as ").append(dataMap.slug()).append(" {\n");
        body(text, 1, dataMap.fields(), dataMap.entries());
        return text.append("}\n").toString();
    }

    private static void body(StringBuilder text, int depth, List<DataMapField> fields, List<DataMapEntry> entries) {
        String indent = INDENT.repeat(depth);
        for (DataMapField field : fields) {
            above(text, indent, field.comments());
            text.append(indent).append(field.name()).append(' ').append(field.value().written());
            field.source().ifPresent(source -> text.append(' ').append(source.written()));
            beside(text, field.comments());
        }
        boolean first = fields.isEmpty();
        for (DataMapEntry entry : entries) {
            if (!first) {
                text.append('\n');
            }
            first = false;
            above(text, indent, entry.comments());
            text.append(indent).append(entry.id());
            entry.title().ifPresent(title -> text.append(' ').append(DataMapValue.quoted(title)));
            entry.source().ifPresent(source -> text.append(' ').append(source.written()));
            text.append(" {");
            beside(text, entry.comments());
            body(text, depth + 1, entry.fields(), entry.entries());
            text.append(indent).append("}\n");
        }
    }

    private static void above(StringBuilder text, String indent, DataMapComments comments) {
        comments.above().forEach(comment -> text.append(indent).append(comment).append('\n'));
    }

    private static void beside(StringBuilder text, DataMapComments comments) {
        comments.beside().ifPresent(comment -> text.append(' ').append(comment));
        text.append('\n');
    }
}
