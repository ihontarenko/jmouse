package org.jmouse.el.grammar;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Trivia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Makes a line of a grammar as its node — the node a reader would have read it into —, a part at a time:
 * {@code LineBuilder.line(PAD).slot("number", Nodes.text("1")).clause("at", Nodes.point(0, 0)).build()}. Every role and
 * clause is checked against the grammar, so a program cannot make a line the grammar would not read.
 */
public final class LineBuilder {

    private final LineGrammar                  grammar;
    private final Map<String, Expression>      slots   = new LinkedHashMap<>();
    private final List<ClauseLineNode.Written> clauses = new ArrayList<>();
    private final List<Trivia>                 above   = new ArrayList<>();
    private final List<String>                 prefixes = new ArrayList<>();
    private Expression ending;

    private LineBuilder(LineGrammar grammar) {
        this.grammar = grammar;
    }

    public static LineBuilder line(LineGrammar grammar) {
        return new LineBuilder(grammar);
    }

    /** A word standing before the line's own, saying how it is meant: {@code live glyph …}. */
    public LineBuilder prefix(String word) {
        if (!grammar.prefixes().contains(word)) {
            throw new IllegalArgumentException("'" + word + "' does not stand before '" + grammar.termWord() + "'");
        }
        prefixes.add(word);
        return this;
    }

    public LineBuilder slot(String role, Expression value) {
        if (grammar.slots().stream().noneMatch(slot -> slot.role().equals(role))) {
            throw new IllegalArgumentException("'" + grammar.termWord() + "' has no slot '" + role + "'");
        }
        slots.put(role, value);
        return this;
    }

    public LineBuilder clause(String word, Expression value) {
        LineGrammar.Clause clause = grammar.clause(word);
        if (clause == null) {
            throw new IllegalArgumentException("'" + grammar.termWord() + "' has no clause '" + word + "'");
        }
        if (value == null && clause.value() != Values.FLAG) {
            throw new IllegalArgumentException("'" + word + "' of '" + grammar.termWord() + "' takes a value");
        }
        clauses.add(new ClauseLineNode.Written(word, value));
        return this;
    }

    /** A clause whose word says it all: {@code filled}. */
    public LineBuilder flag(String word) {
        return clause(word, null);
    }

    public LineBuilder ending(Expression value) {
        if (grammar.ending() == null) {
            throw new IllegalArgumentException("'" + grammar.termWord() + "' is not ended by a text");
        }
        this.ending = value;
        return this;
    }

    /** A comment written above the line, its text without the mark that opens it. */
    public LineBuilder comment(String text) {
        above.add(Lines.comment(text));
        return this;
    }

    public ClauseLineNode build() {
        ClauseLineNode line = new ClauseLineNode(grammar, true, prefixes, slots, clauses, ending);
        line.addLeadingTrivia(above);
        String refusal = grammar.check() == null ? null : grammar.check().apply(line, grammar);
        if (refusal != null) {
            throw new IllegalArgumentException(refusal);
        }
        return line;
    }
}
