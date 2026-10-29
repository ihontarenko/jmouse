package org.jmouse.el.grammar;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor;
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
 * Reads a term — {@code head[:name][(argument, word: value, flag, nested(…), "text")]} — by the {@link LineGrammar} of the
 * line it writes, into the very node the line would be read into: its name the grammar's term name, its positional
 * arguments the grammar's slots in order, its named arguments clauses — or a marked slot, or a part's settings —, a bare
 * word a flag, a nested term a nested clause, a last text in quotes the line's ending.
 */
public final class TermReading {

    /** An argument ends at its comma, or at the parenthesis that closes the arguments. */
    public static final BoundedTokenCursor.Boundary ARGUMENT = (token, next, depth) -> Lines.LINE.endsAt(token, next, depth)
            || depth == 0 && (token.type() == BasicToken.T_COMMA || token.type() == BasicToken.T_CLOSE_PAREN);

    /** A name ends where the term's arguments or body begin. */
    private static final BoundedTokenCursor.Boundary NAME = (token, next, depth) -> Lines.LINE.endsAt(token, next, depth)
            || token.type() == BasicToken.T_OPEN_PAREN;

    private TermReading() {
    }

    /** Whether the cursor stands on a term of this head: the head, then {@code :} or {@code (} against it. */
    public static boolean opens(TokenCursor cursor, String head) {
        if (head == null || !cursor.checkValueAt(0, head) || Lines.endsAt(cursor, 1)) {
            return false;
        }
        Token after = cursor.lookAt(1);
        boolean marked = after.type() == BasicToken.T_COLON || after.type() == BasicToken.T_OPEN_PAREN;
        return marked && SourceReading.touches(cursor, cursor.current(), after);
    }

    /** A line written as a term, its head already read. */
    public static ClauseLineNode line(LineGrammar grammar, boolean headed, TokenCursor cursor, ParserContext context, Token head) {
        Map<String, Expression> slots = new LinkedHashMap<>();
        if (cursor.isCurrent(BasicToken.T_COLON)) {
            cursor.next();
            if (grammar.termName() == null) {
                throw GrammarRefusal.at(cursor, head, "'" + grammar.termWord() + "' takes no name: " + grammar.shape());
            }
            LineGrammar.Slot slot = slot(grammar, grammar.termName());
            slots.put(slot.role(), slot.value().readTerm(new ValueReading(cursor, new BoundedTokenCursor(cursor, NAME), context,
                    Set.of(), slot.allowed(), head, grammar)));
        }
        return arguments(grammar, headed, slots, cursor, context, head);
    }

    /**
     * The arguments in parentheses where the cursor stands — none when no parenthesis does —, read against a grammar
     * into its line's node.
     *
     * @param slots what the term's name already filled
     */
    public static ClauseLineNode arguments(LineGrammar grammar, boolean headed, Map<String, Expression> slots,
                                           TokenCursor cursor, ParserContext context, Token head) {
        Set<String> words = grammar.clauseWords();
        List<LineGrammar.Slot> positional = grammar.slots().stream()
                .filter(slot -> !slot.role().equals(grammar.termName()))
                .filter(slot -> slot.namedBy() == null || slot.mark() != null && slot.mark().equals(grammar.termWord())
                        && slot.termWord() == null).toList();
        LineGrammar.Slot settingsSlot = grammar.slots().stream().filter(slot -> slot.value() == Values.SETTINGS).findFirst()
                .orElse(null);
        List<SettingsNode.Setting> settings = new ArrayList<>();
        List<ClauseLineNode.Written> clauses = new ArrayList<>();
        Set<String> written = new HashSet<>();
        Expression ending = null;
        int endingAt = -1;
        List<String> prefixes = new ArrayList<>();
        int next = 0;
        if (cursor.isCurrent(BasicToken.T_OPEN_PAREN)) {
            Token open = cursor.next();
            while (!cursor.isCurrent(BasicToken.T_CLOSE_PAREN)) {
                if (Lines.lineEnd().matches(cursor)) {
                    throw GrammarRefusal.at(cursor, open, "a term's arguments close with ')': " + grammar.shape());
                }
                if (cursor.isCurrent(BasicToken.T_COMMA)) {
                    cursor.next();
                    continue;
                }
                Token at = cursor.current();
                if (named(cursor)) {
                    String word = Lines.word(cursor);
                    cursor.next();
                    ValueReading reading = new ValueReading(cursor, new BoundedTokenCursor(cursor, ARGUMENT), context, words,
                            Set.of(), at, grammar);
                    LineGrammar.Clause clause = grammar.clause(word);
                    LineGrammar.Slot marked = grammar.slots().stream().filter(slot -> word.equals(slot.namedBy())).findFirst()
                            .orElse(null);
                    if (clause != null && !clause.isNested()) {
                        once(cursor, at, word, clause, written);
                        clauses.add(new ClauseLineNode.Written(word, clause.value().readTerm(new ValueReading(cursor,
                                reading.bounded(), context, words, clause.allowed(), at, grammar))));
                    } else if (marked != null) {
                        slots.put(marked.role(), marked.value().readTerm(reading));
                    } else if (settingsSlot != null) {
                        settings.add(new SettingsNode.Setting(word, cursor.isCurrent(BasicToken.T_STRING) ? reading.quoted()
                                : reading.formula()));
                    } else {
                        throw GrammarRefusal.at(cursor, at, "'" + word + "' is no argument of this term: " + grammar.shape());
                    }
                    continue;
                }
                if (grammar.prefixes().contains(cursor.current().value()) && endsArgument(cursor, 1)) {
                    prefixes.add(cursor.next().value());
                    continue;
                }
                LineGrammar.Clause clause = grammar.clause(cursor.current().value());
                if (clause != null && clause.isNested() && cursor.lookAt(1).type() == BasicToken.T_OPEN_PAREN) {
                    once(cursor, at, clause.word(), clause, written);
                    cursor.next();
                    clauses.add(new ClauseLineNode.Written(clause.word(),
                            arguments(clause.nested(), false, new LinkedHashMap<>(), cursor, context, at)));
                    continue;
                }
                if (clause != null && clause.value() == Values.FLAG && endsArgument(cursor, 1)) {
                    once(cursor, at, clause.word(), clause, written);
                    cursor.next();
                    clauses.add(new ClauseLineNode.Written(clause.word(), null));
                    continue;
                }
                boolean placed = false;
                while (next < positional.size()) {
                    LineGrammar.Slot slot = positional.get(next);
                    if (slot.value() == Values.SETTINGS) {
                        settings.add(new SettingsNode.Setting(Lines.word(cursor), null));
                        placed = true;
                        break;
                    }
                    next++;
                    if (slot.optional() && !slot.value().standsAtTerm(cursor, words, slot.allowed())) {
                        continue;
                    }
                    slots.put(slot.role(), slot.value().readTerm(new ValueReading(cursor, new BoundedTokenCursor(cursor, ARGUMENT),
                            context, words, slot.allowed(), at, grammar)));
                    placed = true;
                    break;
                }
                if (placed) {
                    continue;
                }
                if (cursor.isCurrent(BasicToken.T_STRING) && grammar.ending() != null && ending == null) {
                    ending = grammar.endingValue().readTerm(new ValueReading(cursor, new BoundedTokenCursor(cursor, ARGUMENT),
                            context, words, Set.of(), at, grammar));
                    endingAt = clauses.size();
                    continue;
                }
                throw GrammarRefusal.at(cursor, at, "'" + at.value() + "' is not an argument of this term: " + grammar.shape());
            }
            cursor.next();
        }
        if (settingsSlot != null && !settings.isEmpty()) {
            slots.put(settingsSlot.role(), new SettingsNode(settings));
        }
        for (LineGrammar.Slot slot : grammar.slots()) {
            if (!slot.optional() && !slots.containsKey(slot.role())) {
                throw GrammarRefusal.at(cursor, head, grammar.shape());
            }
        }
        for (LineGrammar.Clause clause : grammar.clauses()) {
            if (clause.required() && !written.contains(clause.word()) && !clause.repeatable()) {
                throw GrammarRefusal.at(cursor, head, grammar.shape());
            }
        }
        ClauseLineNode node = new ClauseLineNode(grammar, headed, prefixes, slots, clauses, ending,
                endingAt < 0 ? clauses.size() : endingAt);
        String refusal = grammar.check() == null ? null : grammar.check().apply(node, grammar);
        if (refusal != null) {
            throw GrammarRefusal.at(cursor, head, refusal);
        }
        return node;
    }

    /** {@code word: value}: a word, then a colon against it. */
    private static boolean named(TokenCursor cursor) {
        TokenCursor.Savepoint savepoint = cursor.savepoint();
        boolean named = false;
        if (!cursor.isCurrent(BasicToken.T_STRING) && !Lines.lineEnd().matches(cursor)) {
            SourceReading.touchingWord(cursor, Lines.WORD_SEPARATORS);
            named = cursor.hasNext() && cursor.isCurrent(BasicToken.T_COLON);
        }
        cursor.restore(savepoint);
        return named;
    }

    /** Whether the argument ends {@code offset} tokens ahead: its comma, the closing parenthesis. */
    private static boolean endsArgument(TokenCursor cursor, int offset) {
        Token token = cursor.lookAt(offset);
        return token == null || token.type() == BasicToken.T_COMMA || token.type() == BasicToken.T_CLOSE_PAREN;
    }

    private static void once(TokenCursor cursor, Token at, String word, LineGrammar.Clause clause, Set<String> written) {
        if (!clause.repeatable() && !written.add(word)) {
            throw GrammarRefusal.at(cursor, at, "'" + word + "' is written twice");
        }
    }

    private static LineGrammar.Slot slot(LineGrammar grammar, String role) {
        return grammar.slots().stream().filter(slot -> slot.role().equals(role)).findFirst().orElseThrow();
    }
}
