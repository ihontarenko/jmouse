package org.jmouse.parallax.script;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Trivia;
import org.jmouse.el.node.expression.SpanNode;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything a read jMX document holds, written out as text, so that two readers can be compared by what they built
 * rather than by how: every record component in order, every {@link SourceText} with its line, column and offset, and
 * every jMS node with its kind, its span and its children.
 */
final class DocumentSnapshot {

    private DocumentSnapshot() {
    }

    static String of(List<ExperienceDocument> documents) {
        StringBuilder snapshot = new StringBuilder();
        for (ExperienceDocument document : documents) {
            write(snapshot, document, 0);
        }
        return snapshot.toString();
    }

    /** What reading a text ends in: its documents, or the refusal with where it stands. */
    static String outcome(Reader reader, String text, String document) {
        try {
            return of(reader.read(text, document));
        } catch (ExperienceParseException refusal) {
            return "REFUSED " + refusal.getDocument() + " " + refusal.getLine() + ":" + refusal.getColumn() + " "
                    + refusal.getDetail();
        }
    }

    @FunctionalInterface
    interface Reader {
        List<ExperienceDocument> read(String text, String document);
    }

    private static void write(StringBuilder snapshot, Object value, int depth) {
        String indent = "  ".repeat(depth);
        switch (value) {
            case null -> snapshot.append("null\n");
            case SourceText text -> snapshot.append("SourceText[").append(text.line()).append(':').append(text.column())
                    .append('@').append(text.offset()).append("] ").append(quoted(text.text())).append('\n');
            case String text -> snapshot.append(quoted(text)).append('\n');
            case Number number -> snapshot.append(number.getClass().getSimpleName()).append(' ').append(number).append('\n');
            case Boolean bool -> snapshot.append(bool).append('\n');
            case Character character -> snapshot.append('\'').append(character).append("'\n");
            case Enum<?> constant -> snapshot.append(constant.getDeclaringClass().getSimpleName()).append('.')
                    .append(constant.name()).append('\n');
            case Optional<?> optional -> {
                if (optional.isEmpty()) {
                    snapshot.append("Optional.empty\n");
                } else {
                    snapshot.append("Optional ");
                    write(snapshot, optional.get(), depth);
                }
            }
            case Map<?, ?> map -> {
                snapshot.append("Map(").append(map.size()).append(")\n");
                map.forEach((key, entry) -> {
                    snapshot.append(indent).append("  ").append(key).append(" = ");
                    write(snapshot, entry, depth + 2);
                });
            }
            case Collection<?> collection -> {
                snapshot.append("List(").append(collection.size()).append(")\n");
                for (Object item : collection) {
                    snapshot.append(indent).append("  - ");
                    write(snapshot, item, depth + 2);
                }
            }
            case Record record -> {
                snapshot.append(record.getClass().getSimpleName()).append('\n');
                for (RecordComponent component : record.getClass().getRecordComponents()) {
                    snapshot.append(indent).append("  ").append(component.getName()).append(": ");
                    write(snapshot, valueOf(record, component), depth + 2);
                }
            }
            case Node node -> writeNode(snapshot, node, depth);
            case double[] numbers -> snapshot.append(Arrays.toString(numbers)).append('\n');
            case int[] numbers -> snapshot.append(Arrays.toString(numbers)).append('\n');
            default -> snapshot.append(value.getClass().getSimpleName()).append(' ').append(value).append('\n');
        }
    }

    private static void writeNode(StringBuilder snapshot, Node node, int depth) {
        String indent = "  ".repeat(depth);
        snapshot.append(node.getClass().getSimpleName());
        if (node instanceof AbstractExpression expression && expression.getSpan() instanceof SpanNode span) {
            snapshot.append(" @").append(span.lineNumber()).append(':').append(span.position());
        }
        String shown = String.valueOf(node);
        if (!shown.contains("@")) {
            snapshot.append(' ').append(quoted(shown));
        }
        snapshot.append('\n');
        for (Trivia trivia : node.getLeadingTrivia()) {
            snapshot.append(indent).append("  ~ ").append(trivia).append('\n');
        }
        if (node.getTrailingTrivia() != null) {
            snapshot.append(indent).append("  ~> ").append(node.getTrailingTrivia()).append('\n');
        }
        for (Node child : node.getChildren()) {
            snapshot.append(indent).append("  * ");
            writeNode(snapshot, child, depth + 2);
        }
    }

    private static Object valueOf(Record record, RecordComponent component) {
        try {
            component.getAccessor().setAccessible(true);
            return component.getAccessor().invoke(record);
        } catch (ReflectiveOperationException unreadable) {
            throw new IllegalStateException("cannot read " + component.getName() + " of " + record, unreadable);
        }
    }

    private static String quoted(String text) {
        return '"' + text.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"") + '"';
    }
}
