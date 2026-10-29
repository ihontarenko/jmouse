package org.jmouse.el.grammar;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Trivia;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.el.writer.SourceWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Writes statements read by a grammar as terms — {@code head:name(argument, word: value, flag) { body }} —, from the same
 * tree the grammar's own writing comes from. A line's grammar says everything a term needs: the name a named line opens
 * with is the term's name, its slots its arguments in order, its clauses named arguments, a flag a bare word, a nested
 * clause a term of its own. A language subclasses it for the statements no grammar reads ({@link #other}).
 */
public class TermWriter {

    /** Between a term's head and its name, and after a named argument's word. */
    protected static final String COLON = ":";

    /** A name a term writes bare: letters, digits, and what joins them in a name — {@code q[0..7]}, {@code U1.lit}. */
    private static final Pattern PLAIN_NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.\\-\\[\\]]*");

    private final boolean flat;

    /** A writer that keeps every block a body holds. */
    public TermWriter() {
        this(false);
    }

    /**
     * @param flat whether a block that only gathers terms of one kind — {@code parameters { … }} — is left out, its terms
     *             standing in the body that holds it, each saying what it is by its head
     */
    public TermWriter(boolean flat) {
        this.flat = flat;
    }

    /** One statement on a line of its own, with what its author wrote above and beside it. */
    public void statement(SourceWriter writer, Expression statement) {
        StatementWriting.leading(writer, statement);
        line(writer, statement);
        Trivia trailing = statement.getTrailingTrivia();
        if (trailing != null) {
            writer.word(trailing.text());
        }
        writer.endLine();
    }

    /** The statements of a body. */
    public void statements(SourceWriter writer, Iterable<? extends Expression> statements) {
        for (Expression statement : statements) {
            if (flat && gathers(statement)) {
                StatementWriting.leading(writer, statement);
                statements(writer, ((ClauseBlockNode) statement).getExpressions());
            } else {
                statement(writer, statement);
            }
        }
    }

    /** Whether a statement is a block with no head that only gathers named terms: its terms say what it says. */
    private static boolean gathers(Expression statement) {
        return statement instanceof ClauseBlockNode block && block.header().isEmpty() && block.opened()
                && !block.getExpressions().isEmpty() && block.getExpressions().stream()
                .allMatch(line -> line instanceof ClauseLineNode clause && clause.grammar().isNamed());
    }

    protected void line(SourceWriter writer, Expression statement) {
        switch (statement) {
            case ClauseLineNode line -> clause(writer, line);
            case ClauseBlockNode block -> block(writer, block);
            default -> other(writer, statement);
        }
    }

    /** A statement no grammar reads: written as it writes itself. */
    protected void other(SourceWriter writer, Expression statement) {
        statement.writeSource(writer);
    }

    private void clause(SourceWriter writer, ClauseLineNode line) {
        LineGrammar grammar = line.grammar();
        if (grammar.asWritten()) {
            line.writeSource(writer);
            return;
        }
        WordNode name = grammar.termName() == null ? null : line.slot(grammar.termName()).map(WordNode.class::cast).orElse(null);
        namedTerm(writer, grammar.termWord(), name, arguments(line, grammar.termName()));
    }

    private void block(SourceWriter writer, ClauseBlockNode block) {
        List<Consumer<SourceWriter>> arguments = block.header().map(header -> arguments(header, header.grammar().termName()))
                .orElse(List.of());
        WordNode name = block.header().filter(header -> header.grammar().termName() != null)
                .flatMap(header -> header.slot(header.grammar().termName())).map(WordNode.class::cast).orElse(null);
        namedTerm(writer, block.header().map(header -> header.grammar().termWord()).orElse(block.word()), name, arguments);
        if (block.opened()) {
            writer.block(body -> statements(body, block.getExpressions()));
        }
    }

    /** {@code head}, {@code head:name}, {@code head(arguments)} or {@code head:name(arguments)}. */
    protected void term(SourceWriter writer, String head, String name, List<Consumer<SourceWriter>> arguments) {
        namedTerm(writer, head, name == null ? null : new WordNode(name, false), arguments);
    }

    /** A name that is no plain name — an empty pad number — is written in quotes: {@code pad:""(…)}. */
    private void namedTerm(SourceWriter writer, String head, WordNode name, List<Consumer<SourceWriter>> arguments) {
        writer.word(head);
        if (name != null) {
            writer.join(COLON);
            if (PLAIN_NAME.matcher(name.word()).matches()) {
                writer.join(name.word());
            } else {
                writer.text(name.word());
            }
        }
        if (!arguments.isEmpty()) {
            writer.opening("(");
            for (int index = 0; index < arguments.size(); index++) {
                if (index > 0) {
                    writer.join(",");
                }
                arguments.get(index).accept(writer);
            }
            writer.join(")");
        }
    }

    /**
     * A line's slots but the one written as the term's name, then its clauses, then the text that ends it. A slot whose
     * mark is the term's own word — {@code wire} of {@code W1 wire R1.a} — is written without it: the head says it.
     */
    private List<Consumer<SourceWriter>> arguments(ClauseLineNode line, String nameRole) {
        LineGrammar grammar = line.grammar();
        List<Consumer<SourceWriter>> arguments = new ArrayList<>();
        line.prefixes().forEach(prefix -> arguments.add(writer -> writer.word(prefix)));
        for (LineGrammar.Slot slot : grammar.slots()) {
            if (slot.role().equals(nameRole)) {
                continue;
            }
            boolean headMarked = slot.mark() != null && slot.mark().equals(grammar.termWord());
            String namedBy = headMarked ? slot.termWord() : slot.namedBy();
            line.slot(slot.role()).ifPresent(value -> {
                if (value instanceof SettingsNode settings) {
                    settings.settings().forEach(setting -> arguments.add(setting.given()
                            .<Consumer<SourceWriter>>map(given -> named(setting.name(), given))
                            .orElse(writer -> writer.word(setting.name()))));
                } else if (namedBy != null) {
                    arguments.add(named(namedBy, value));
                } else {
                    arguments.add(writer -> value(writer, value));
                }
            });
        }
        for (ClauseLineNode.Written clause : line.clauses()) {
            Expression value = clause.value();
            if (value == null) {
                arguments.add(writer -> writer.word(clause.word()));
            } else if (value instanceof ClauseLineNode nested) {
                arguments.add(writer -> term(writer, clause.word(), null, arguments(nested, null)));
            } else {
                arguments.add(named(clause.word(), value));
            }
        }
        line.endingValue().ifPresent(ending -> arguments.add(arguments.size() - (line.clauses().size() - line.endingAt()),
                writer -> value(writer, ending)));
        return arguments;
    }

    private Consumer<SourceWriter> named(String word, Expression value) {
        return writer -> {
            writer.word(word).join(COLON);
            value(writer, value);
        };
    }

    /** A value as an argument: a list in brackets, a point as its two numbers, the rest as it writes itself. */
    protected void value(SourceWriter writer, Expression value) {
        switch (value) {
            case AssignedNode assigned -> value(writer, assigned.value());
            case PointNode point -> point.coordinates().forEach(writer::write);
            case ListNode list -> list(writer, list);
            case ConnectionsNode connections -> connections(writer, connections);
            case SettingsNode settings -> settings(writer, settings);
            case FormulaNode formula -> writer.word(formula.written());
            case QuantityLiteralNode quantity -> writer.write(quantity);
            default -> writer.write(value);
        }
    }

    /** One value alone; several in brackets; a group of words — {@code 1 7}, {@code clock edge} — a space apart. */
    private void list(SourceWriter writer, ListNode list) {
        List<Expression> items = list.items();
        if (items.size() == 1) {
            value(writer, items.getFirst());
            return;
        }
        if (!list.commas()) {
            items.forEach(item -> value(writer, item));
            return;
        }
        writer.openingWord("[");
        for (int index = 0; index < items.size(); index++) {
            if (index > 0) {
                writer.join(",");
            }
            value(writer, items.get(index));
        }
        writer.join("]");
    }

    /** {@code [flipped: 1, inverted: 1]}: settings as a clause's value, a list of named values. */
    private void settings(SourceWriter writer, SettingsNode settings) {
        writer.openingWord("[");
        List<SettingsNode.Setting> all = settings.settings();
        for (int index = 0; index < all.size(); index++) {
            SettingsNode.Setting setting = all.get(index);
            if (index > 0) {
                writer.join(",");
            }
            writer.word(setting.name());
            setting.given().ifPresent(given -> {
                writer.join(COLON);
                value(writer, given);
            });
        }
        writer.join("]");
    }

    /** {@code [in, out]}, or {@code [positive: vcc, negative: 0]}. */
    private void connections(SourceWriter writer, ConnectionsNode connections) {
        writer.openingWord("[");
        List<ConnectionsNode.Connection> all = connections.connections();
        for (int index = 0; index < all.size(); index++) {
            ConnectionsNode.Connection connection = all.get(index);
            if (index > 0) {
                writer.join(",");
            }
            if (connection.pin() != null) {
                writer.word(connection.pin()).join(COLON);
            }
            writer.word(connection.net());
        }
        writer.join("]");
    }
}
