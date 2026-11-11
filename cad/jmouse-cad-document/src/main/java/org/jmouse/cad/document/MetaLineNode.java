package org.jmouse.cad.document;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.grammar.QuotedText;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.writer.SourceLayout;
import org.jmouse.el.writer.SourceWritable;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.el.writer.TextSourceWriter;

import java.util.List;

/**
 * One field of {@code meta}: its key and its value as the author wrote it — {@code summary "A diode"},
 * {@code tags logic, lesson}, {@code supply_range 4.5 V .. 16 V} —, or a group of lines under the key in braces
 * ({@code links { "label" "https://…" }}). Data, not a formula: the value is kept, never rebuilt.
 */
public class MetaLineNode extends AbstractExpression {

    private final String       key;
    private static final String LONG_QUOTES = "\"\"\"";

    private final String       written;
    private final List<String> group;

    /**
     * @param written the value as written; empty for a group
     * @param group   the lines of a group as written, each whole; empty for a value
     */
    public MetaLineNode(String key, String written, List<String> group) {
        this.key = key;
        this.written = written;
        this.group = List.copyOf(group);
    }

    /** A field a program makes: its value a node — a text, a word, a list —, written as that node writes itself. */
    public static MetaLineNode of(String key, SourceWritable value) {
        return new MetaLineNode(key, TextSourceWriter.render(value, SourceLayout.WORDS), List.of());
    }

    /** A group a program makes — {@code links { "Datasheet" "https://…" }} —: each line its nodes, as they write themselves. */
    public static MetaLineNode group(String key, List<? extends SourceWritable> lines) {
        return new MetaLineNode(key, "", lines.stream().map(line -> TextSourceWriter.render(line, SourceLayout.WORDS)).toList());
    }

    public String key() {
        return key;
    }

    public String written() {
        return written;
    }

    public List<String> group() {
        return group;
    }

    /**
     * The value without the quotes it is written in, when it is one text in quotes — a {@code """ … """} one dedented, as
     * the reader takes every long text —; as written otherwise.
     */
    public String text() {
        if (written.length() >= 2 * LONG_QUOTES.length() && written.startsWith(LONG_QUOTES) && written.endsWith(LONG_QUOTES)) {
            return QuotedText.dedented(written.substring(LONG_QUOTES.length(), written.length() - LONG_QUOTES.length()));
        }
        boolean quoted = written.length() >= 2 && written.charAt(0) == '"' && written.charAt(written.length() - 1) == '"'
                && written.indexOf('"', 1) == written.length() - 1;
        return quoted ? written.substring(1, written.length() - 1) : written;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return written;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        writer.word(key);
        if (group.isEmpty()) {
            writer.word(written);
            return;
        }
        writer.block(body -> group.forEach(line -> body.word(line).endLine()));
    }
}
