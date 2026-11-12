package org.jmouse.licence.reading;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.SyntaxErrorException;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.licence.LicenceExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads an SPDX licence expression on the expression engine's lexer. An identifier is a word read whole wherever its tokens
 * touch — {@code CC-BY-SA-4.0} is one word, though the lexer cuts it at each dash —, ended by a blank or a bracket. The
 * operators are words standing apart, in SPDX's order of binding: {@code WITH}, then {@code AND}, then {@code OR}; brackets
 * group.
 *
 * <pre>
 * expression  = all ("OR" all)*
 * all         = licence ("AND" licence)*
 * licence     = "(" expression ")" | identifier ["+"] ["WITH" identifier]
 * </pre>
 */
public final class LicenceExpressionReader {

    private static final String OPEN  = "(";
    private static final String CLOSE = ")";
    private static final String AND   = "AND";
    private static final String OR    = "OR";
    private static final String WITH  = "WITH";

    /** What ends a word even where it touches it. */
    private static final Set<String> SEPARATORS = Set.of(OPEN, CLOSE);

    private static final Lexer LEXER = new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), new Recognizer()));

    private final String       text;
    private final List<String> pieces;
    private int                position;

    private LicenceExpressionReader(String text, List<String> pieces) {
        this.text = text;
        this.pieces = pieces;
    }

    /**
     * @throws IllegalArgumentException for a text that is not an SPDX expression, saying where
     */
    public static LicenceExpression read(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("A licence expression names one licence at least.");
        }
        LicenceExpressionReader reader = new LicenceExpressionReader(text, pieces(text));
        LicenceExpression expression = reader.expression();
        if (reader.position < reader.pieces.size()) {
            throw reader.refusal("'" + reader.pieces.get(reader.position) + "' follows a whole expression");
        }
        return expression;
    }

    /** The words and brackets of the text, in order. */
    private static List<String> pieces(String text) {
        List<String> pieces = new ArrayList<>();
        try {
            TokenCursor cursor = LEXER.tokenize(new StringSource("licence", text));
            while (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_EOL)) {
                // The marker a lexed stream opens with, and a line break: neither is written in the expression.
                if (cursor.isCurrent(BasicToken.T_SOL, BasicToken.T_NEW_LINE)) {
                    cursor.next();
                    continue;
                }
                Token current = cursor.lookAt(0);
                if (SEPARATORS.contains(current.value())) {
                    pieces.add(cursor.next().value());
                } else {
                    pieces.add(SourceReading.touchingWord(cursor, SEPARATORS));
                }
            }
        } catch (SyntaxErrorException refusal) {
            throw new IllegalArgumentException("'" + text + "' is not a licence expression: " + refusal.getMessage(), refusal);
        }
        return pieces;
    }

    private LicenceExpression expression() {
        List<LicenceExpression> choices = new ArrayList<>(List.of(all()));
        while (at(OR)) {
            position++;
            choices.add(all());
        }
        return choices.size() == 1 ? choices.getFirst() : new LicenceExpression.AnyOf(choices);
    }

    private LicenceExpression all() {
        List<LicenceExpression> parts = new ArrayList<>(List.of(licence()));
        while (at(AND)) {
            position++;
            parts.add(licence());
        }
        return parts.size() == 1 ? parts.getFirst() : new LicenceExpression.AllOf(parts);
    }

    private LicenceExpression licence() {
        if (at(OPEN)) {
            position++;
            LicenceExpression grouped = expression();
            if (!at(CLOSE)) {
                throw refusal("a bracket opened is not closed");
            }
            position++;
            return grouped;
        }
        String identifier = identifier("a licence");
        boolean orLater = identifier.endsWith(LicenceExpression.OR_LATER);
        String licence = orLater ? identifier.substring(0, identifier.length() - LicenceExpression.OR_LATER.length()) : identifier;
        Optional<String> exception = Optional.empty();
        if (at(WITH)) {
            position++;
            exception = Optional.of(identifier("an exception after WITH"));
        }
        return new LicenceExpression.Single(licence, orLater, exception);
    }

    private String identifier(String what) {
        if (position >= pieces.size()) {
            throw refusal(what + " is not named before the end");
        }
        String piece = pieces.get(position);
        if (SEPARATORS.contains(piece) || isOperator(piece)) {
            throw refusal(what + " is not named where '" + piece + "' stands");
        }
        position++;
        return piece;
    }

    private boolean at(String piece) {
        if (position >= pieces.size()) {
            return false;
        }
        String current = pieces.get(position);
        return isOperator(piece) ? current.equalsIgnoreCase(piece) : current.equals(piece);
    }

    /** SPDX's operators, which it reads in either case. */
    private static boolean isOperator(String piece) {
        return piece.equalsIgnoreCase(AND) || piece.equalsIgnoreCase(OR) || piece.equalsIgnoreCase(WITH);
    }

    private IllegalArgumentException refusal(String detail) {
        return new IllegalArgumentException("'" + text + "' is not a licence expression: " + detail + ".");
    }

    /** The expression engine's own token kinds: SPDX gives no word a kind of its own, its operators are read as words. */
    private static final class Recognizer extends CompositeRecognizer {

        private Recognizer() {
            addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 3000));
        }
    }
}
