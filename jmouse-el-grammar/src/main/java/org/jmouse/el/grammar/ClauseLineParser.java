package org.jmouse.el.grammar;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ParserContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads a line by its {@link LineGrammar}: its word — or the name it opens with —, its slots in order, its clauses in any
 * order, the text that may end it. Each value is read by its {@link ValueKind}, through a cursor bounded where the
 * grammar's next word, a text in quotes or the line's end stands.
 *
 * <p>One instance per grammar, asked by the body that holds the line ({@link LineSet}); never registered in the context,
 * which holds one parser per class.</p>
 */
public class ClauseLineParser implements LineReader {

    /** How a line is read: a line of its own, the clauses after a nested clause's word, or the head of a block. */
    private enum Reading {
        LINE, NESTED, HEADER
    }

    private final LineGrammar grammar;

    public ClauseLineParser(LineGrammar grammar) {
        this.grammar = grammar;
    }

    public LineGrammar grammar() {
        return grammar;
    }

    @Override
    public boolean opens(TokenCursor cursor) {
        int prefixes = grammar.prefixesAt(cursor);
        if (!grammar.isNamed()) {
            return cursor.checkValueAt(prefixes, grammar.word());
        }
        if (Lines.endsAt(cursor, prefixes) || cursor.lookAt(prefixes).type() == BasicToken.T_STRING) {
            return false;
        }
        if (grammar.slots().size() < 2) {
            return true;
        }
        TokenCursor.Savepoint savepoint = cursor.savepoint();
        cursor.shift(prefixes);
        boolean told = told(cursor, grammar.slots().get(1));
        cursor.restore(savepoint);
        return told;
    }

    /**
     * Whether what stands after the name is what a named line's second slot says it must be — its mark, one of its
     * words —: how lines that all open with a name are told apart ({@code W1 wire …}, {@code GND1 ground …}, {@code R1 at …}).
     * A second slot that may be left out, or takes any word, tells nothing, and the line opens on any name.
     */
    private static boolean told(TokenCursor cursor, LineGrammar.Slot second) {
        if (second.optional() || second.mark() == null && second.allowed().isEmpty()) {
            return true;
        }
        TokenCursor.Savepoint savepoint = cursor.savepoint();
        Lines.skipName(cursor);
        boolean told = !Lines.lineEnd().matches(cursor)
                && (second.mark() != null ? cursor.current().value().equals(second.mark())
                : second.allowed().contains(Lines.peekWord(cursor)));
        cursor.restore(savepoint);
        return told;
    }

    /** A term of its word — {@code pin:out(…)}, {@code measure(…)} —, unless terms keep the line as it is written. */
    @Override
    public boolean opensTerm(TokenCursor cursor) {
        return !grammar.asWritten() && TermReading.opens(cursor, grammar.termWord());
    }

    @Override
    public ClauseLineNode readTerm(TokenCursor cursor, ParserContext context) {
        Token head = cursor.next();
        return TermReading.line(grammar, true, cursor, context, head);
    }

    /** What stands after a block's word in a term, before its body: {@code ("title", from: "source")}. */
    public ClauseLineNode headerTerm(TokenCursor cursor, ParserContext context, Token word) {
        return TermReading.line(grammar, false, cursor, context, word);
    }

    @Override
    public String says() {
        return grammar.isNamed() ? "a line opening with its name" : grammar.word();
    }

    @Override
    public ClauseLineNode read(TokenCursor cursor, ParserContext context) {
        List<String> prefixes = new ArrayList<>();
        for (int count = grammar.prefixesAt(cursor); count > 0; count--) {
            prefixes.add(cursor.next().value());
        }
        Token first = grammar.isNamed() ? cursor.current() : cursor.next();
        return read(grammar, Reading.LINE, prefixes, cursor, context, first, Set.of());
    }

    /**
     * What stands after a block's word, before its body: {@code "title" from "source"}, {@code uses one, other}.
     *
     * @param prefixes the words written before the block's word: {@code live} of {@code live glyph …}
     */
    public ClauseLineNode header(TokenCursor cursor, ParserContext context, Token word, List<String> prefixes) {
        return read(grammar, Reading.HEADER, prefixes, cursor, context, word, Set.of());
    }

    /**
     * @param outerWords the words of the line a nested clause stands in: its values end at them too, so {@code name at
     *                   -52, -32 value at 52, -32} ends the first point before {@code value}
     */
    private ClauseLineNode read(LineGrammar line, Reading reading, List<String> prefixes, TokenCursor cursor,
                                ParserContext context, Token first, Set<String> outerWords) {
        Set<String> ownWords = line.clauseWords();
        Set<String> clauseWords = new HashSet<>(ownWords);
        clauseWords.addAll(outerWords);
        Map<String, Expression> slots = new LinkedHashMap<>();
        List<LineGrammar.Slot> lineSlots = line.slots();
        for (int index = 0; index < lineSlots.size(); index++) {
            LineGrammar.Slot slot = lineSlots.get(index);
            boolean opening = index == 0 && line.isNamed();
            if (!opening && !stands(slot, cursor, clauseWords)) {
                if (slot.optional()) {
                    continue;
                }
                throw GrammarRefusal.at(cursor, first, line.shape());
            }
            if (slot.mark() != null) {
                cursor.next();
            }
            Set<String> stops = stops(clauseWords, lineSlots, index);
            slots.put(slot.role(), slot.value().read(new ValueReading(cursor, LineBoundaries.beforeWordsOrText(cursor, stops),
                    context, stops, slot.allowed(), first, line)));
        }
        List<ClauseLineNode.Written> clauses = new ArrayList<>();
        Set<String> written = new HashSet<>();
        Expression ending = null;
        int endingAt = -1;
        while (!Lines.lineEnd().matches(cursor)) {
            Token token = cursor.current();
            if (token.type() == BasicToken.T_STRING) {
                if (line.ending() == null || ending != null) {
                    throw GrammarRefusal.at(cursor, token, line.shape());
                }
                ending = line.endingValue().read(new ValueReading(cursor, LineBoundaries.line(cursor), context, clauseWords,
                        Set.of(), token, line));
                endingAt = clauses.size();
                continue;
            }
            if (reading == Reading.NESTED && !ownWords.contains(token.value())) {
                break;
            }
            String word = SourceReading.touchingWord(cursor, Lines.WORD_SEPARATORS);
            LineGrammar.Clause clause = line.clause(word);
            if (clause == null) {
                throw GrammarRefusal.at(cursor, token, "'" + word + "' is not part of this line: " + line.shape());
            }
            if (!clause.repeatable() && !written.add(word)) {
                throw GrammarRefusal.at(cursor, token, "'" + word + "' is written twice");
            }
            Expression value = clause.isNested()
                    ? read(clause.nested(), Reading.NESTED, List.of(), cursor, context, token, clauseWords)
                    : clause.value().read(new ValueReading(cursor, LineBoundaries.beforeWordsOrText(cursor, clauseWords),
                            context, clauseWords, clause.allowed(), token, line));
            clauses.add(new ClauseLineNode.Written(word, value));
        }
        for (LineGrammar.Clause clause : line.clauses()) {
            if (clause.required() && !written.contains(clause.word()) && !clause.repeatable()) {
                throw GrammarRefusal.at(cursor, first, line.shape());
            }
        }
        ClauseLineNode node = new ClauseLineNode(line, reading == Reading.LINE, prefixes, slots, clauses, ending,
                endingAt < 0 ? clauses.size() : endingAt);
        String refusal = line.check() == null ? null : line.check().apply(node, line);
        if (refusal != null) {
            throw GrammarRefusal.at(cursor, first, refusal);
        }
        return node;
    }

    /**
     * Where a slot's value ends: at the grammar's words, and at the words a later slot may take — {@code = 0 discrete}
     * ends its expression before {@code discrete}.
     */
    private static Set<String> stops(Set<String> clauseWords, List<LineGrammar.Slot> slots, int index) {
        Set<String> stops = new HashSet<>(clauseWords);
        slots.subList(index + 1, slots.size()).forEach(later -> {
            stops.addAll(later.allowed());
            if (later.mark() != null) {
                stops.add(later.mark());
            }
        });
        return stops;
    }

    /** Whether a slot is written where the cursor stands: its mark, or a value of its kind. */
    private static boolean stands(LineGrammar.Slot slot, TokenCursor cursor, Set<String> clauseWords) {
        if (Lines.lineEnd().matches(cursor)) {
            return false;
        }
        if (slot.mark() != null) {
            return cursor.current().value().equals(slot.mark());
        }
        return slot.value().standsAt(cursor, clauseWords, slot.allowed());
    }
}
