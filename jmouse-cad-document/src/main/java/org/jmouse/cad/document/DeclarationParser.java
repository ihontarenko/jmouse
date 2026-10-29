package org.jmouse.cad.document;

import org.jmouse.el.grammar.Lines;

import org.jmouse.el.grammar.GrammarRefusal;
import org.jmouse.el.grammar.LineParser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.parser.ParserContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * {@code extends base}, {@code units A, B}, {@code use one, other}: what a document is built on, each name one word.
 */
public class DeclarationParser extends LineParser<DeclarationNode> {

    /** {@code extends base}: the one component this one is a kind of. */
    public static final String EXTENDS = "extends";
    /** {@code units A, B}: the units a package holds. */
    public static final String UNITS = "units";
    /** {@code use one, other}: the families and function libraries it takes. */
    public static final String USE = "use";

    /** The declarations a body may write. */
    public static final Set<String> WORDS = Set.of(EXTENDS, UNITS, USE);

    /** Whether the cursor stands on a declaration. */
    public boolean declares(TokenCursor cursor) {
        return WORDS.stream().anyMatch(word -> DocumentMatcher.declaration(word).matches(cursor));
    }

    @Override
    protected DeclarationNode line(TokenCursor cursor, ParserContext context) {
        Token word = cursor.next();
        List<String> names = new ArrayList<>();
        while (!Lines.lineEnd().matches(cursor)) {
            // A term writes it 'use:[smooth-transitions, junctions]': its colon and brackets only separate.
            if (cursor.isCurrent(BasicToken.T_COMMA) || cursor.isCurrent(BasicToken.T_COLON)
                    || cursor.isCurrent(BasicToken.T_OPEN_BRACKET) || cursor.isCurrent(BasicToken.T_CLOSE_BRACKET)) {
                cursor.next();
                continue;
            }
            names.add(SourceReading.touchingWord(cursor, Lines.WORD_SEPARATORS));
        }
        if (names.isEmpty()) {
            throw GrammarRefusal.at(cursor, word, "'" + word.value() + "' takes names separated by commas");
        }
        return new DeclarationNode(word.value(), names);
    }
}
