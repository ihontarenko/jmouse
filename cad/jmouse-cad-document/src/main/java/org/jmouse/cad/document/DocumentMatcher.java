package org.jmouse.cad.document;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.grammar.TermReading;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;

/**
 * The shapes of a document's own statements, as lookaheads over tokens — anchored at the cursor, never scanning ahead for
 * a token they hope to find. No word of the language is a token of the lexer: a block is its word at the start of a
 * statement of a body, so {@code state} or {@code view} stays free as a name inside a jMS body. What ends a line or a
 * word is the grammar library's ({@link org.jmouse.el.grammar.Lines}).
 */
public final class DocumentMatcher {

    private DocumentMatcher() {
    }

    /** {@code symbol "Resistor" …}: a kind's word, then the title in quotes. */
    public static Matcher<TokenCursor> document(DocumentGrammar grammar) {
        return cursor -> cursor.isCurrent(BasicToken.T_IDENTIFIER) && grammar.kind(cursor.valueAt(0)).isPresent()
                && cursor.isNext(BasicToken.T_STRING);
    }

    /** {@code component:and-gate(…)}: a kind's word, then a colon against it — a document written as a term. */
    public static Matcher<TokenCursor> documentTerm(DocumentGrammar grammar) {
        return cursor -> cursor.isCurrent(BasicToken.T_IDENTIFIER) && grammar.kind(cursor.valueAt(0)).isPresent()
                && TermReading.opens(cursor, cursor.valueAt(0)) && cursor.lookAt(1).type() == BasicToken.T_COLON;
    }

    /** {@code word …} on a line of its own, not opening a block: {@code extends diode}, {@code use cmos-hc}. */
    public static Matcher<TokenCursor> declaration(String word) {
        return cursor -> cursor.checkValueAt(0, word) && !cursor.isNext(BasicToken.T_OPEN_CURLY) && !cursor.atLineBreak();
    }
}
