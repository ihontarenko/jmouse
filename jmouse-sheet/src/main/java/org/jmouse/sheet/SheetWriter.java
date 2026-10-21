package org.jmouse.sheet;

import java.util.List;

/**
 * Writes a sheet document: four spaces to a level, a map's or an entry's fields first and its entries after them, a
 * blank line before each entry that follows something, and every comment back above or beside its line. What
 * {@link SheetReader} reads back is the same map.
 */
public final class SheetWriter {

    private static final String INDENT = "    ";

    private SheetWriter() {
    }

    public static String write(Sheet sheet) {
        StringBuilder text = new StringBuilder();
        sheet.comments().above().forEach(comment -> text.append(comment).append('\n'));
        text.append("sheet ").append(SheetValue.quoted(sheet.title())).append(" as ").append(sheet.slug()).append(" {\n");
        body(text, 1, sheet.fields(), sheet.entries());
        return text.append("}\n").toString();
    }

    private static void body(StringBuilder text, int depth, List<SheetField> fields, List<SheetEntry> entries) {
        String indent = INDENT.repeat(depth);
        for (SheetField field : fields) {
            above(text, indent, field.comments());
            text.append(indent).append(field.name()).append(' ').append(field.value().written());
            field.source().ifPresent(source -> text.append(' ').append(source.written()));
            beside(text, field.comments());
        }
        boolean first = fields.isEmpty();
        for (SheetEntry entry : entries) {
            if (!first) {
                text.append('\n');
            }
            first = false;
            above(text, indent, entry.comments());
            text.append(indent).append(entry.id());
            entry.title().ifPresent(title -> text.append(' ').append(SheetValue.quoted(title)));
            entry.source().ifPresent(source -> text.append(' ').append(source.written()));
            text.append(" {");
            beside(text, entry.comments());
            body(text, depth + 1, entry.fields(), entry.entries());
            text.append(indent).append("}\n");
        }
    }

    private static void above(StringBuilder text, String indent, SheetComments comments) {
        comments.above().forEach(comment -> text.append(indent).append(comment).append('\n'));
    }

    private static void beside(StringBuilder text, SheetComments comments) {
        comments.beside().ifPresent(comment -> text.append(' ').append(comment));
        text.append('\n');
    }
}
