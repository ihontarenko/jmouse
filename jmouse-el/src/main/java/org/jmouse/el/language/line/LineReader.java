package org.jmouse.el.language.line;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionRecognizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a document of a line language on the jME engine: a language whose statements are lines of jME tokens, grouped
 * into blocks by braces — {@code name "title" { … }} —, as jMX, jMD and their kin are written. It hands out
 * {@link TokenLine}s, which know the words of a line the way a person reads them: {@code 74hc595}, {@code link-a},
 * {@code 2.2µs} are one word each, though the expression lexer cuts them into several tokens.
 *
 * <ul>
 *     <li>A line ends at a line break or a {@code ;}, and before a brace;</li>
 *     <li>{@code #} begins a comment to the end of its line — unless it is joined to the token before it, as in a link
 *     {@code data.jmd#supply}.</li>
 * </ul>
 *
 * <p>A refusal names the document, the 1-based line and column, and what is wrong ({@link LineSyntaxException}).</p>
 */
public final class LineReader {

    private static final Lexer LEXER = new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), new ExpressionRecognizer()));

    private final String document;
    private final String text;
    private final StringSource source;
    private final TokenCursor cursor;

    public LineReader(String document, String text) {
        this.document = document;
        this.text = text;
        this.source = new StringSource(document, text);
        this.cursor = LEXER.tokenize(source);
        this.cursor.consumeIf(BasicToken.T_SOL);
    }

    public String document() {
        return document;
    }

    /** The document's text, as given. */
    public String text() {
        return text;
    }

    public boolean atEnd() {
        return !cursor.hasNext() || cursor.isCurrent(BasicToken.T_EOL);
    }

    public Token peek() {
        return cursor.peek();
    }

    public boolean at(BasicToken type) {
        return !atEnd() && cursor.isCurrent(type);
    }

    /** Whether the next token is that word, unquoted. */
    public boolean atWord(String word) {
        return !atEnd() && cursor.isCurrent(BasicToken.T_IDENTIFIER) && cursor.peek().value().equals(word);
    }

    /**
     * The next token, which must be of that type.
     *
     * @param what what is expected there, for the refusal to name
     */
    public Token expect(BasicToken type, String what) {
        if (!at(type)) {
            throw refusal(cursor.peek(), "expected " + what + (atEnd() ? ", the document ends" : ", not '" + cursor.peek().value() + "'"));
        }
        return cursor.next();
    }

    /** Line breaks, {@code ;} and comments between statements. */
    public void skipBlank() {
        while (!atEnd()) {
            if (cursor.isCurrent(BasicToken.T_NEW_LINE, BasicToken.T_SEMICOLON)) {
                cursor.next();
            } else if (atComment()) {
                skipComment();
            } else {
                return;
            }
        }
    }

    /**
     * The tokens to the end of the line, stopping before a brace or a {@code ;}; a comment ends it. The cursor is left on
     * whatever stopped it.
     */
    public TokenLine line() {
        List<Token> tokens = new ArrayList<>();
        while (!atEnd() && !cursor.isCurrent(BasicToken.T_NEW_LINE, BasicToken.T_SEMICOLON, BasicToken.T_OPEN_CURLY,
                BasicToken.T_CLOSE_CURLY)) {
            if (atComment()) {
                int end = cursor.peek().offset();
                skipComment();
                return new TokenLine(this, tokens, end);
            }
            tokens.add(cursor.next());
        }
        return new TokenLine(this, tokens, atEnd() ? text.length() : cursor.peek().offset());
    }

    /** A refusal at a token. */
    public LineSyntaxException refusal(Token at, String detail) {
        int offset = Math.min(Math.max(at.offset(), 0), text.length());
        return new LineSyntaxException(document, source.getLineNumber(offset), source.getColumnNumber(offset), detail);
    }

    /** A refusal where the cursor stands. */
    public LineSyntaxException refusal(String detail) {
        return refusal(cursor.peek(), detail);
    }

    /** A {@code #} not joined to the token before it: a comment, not part of a word. */
    private boolean atComment() {
        if (!cursor.isCurrent(BasicToken.T_HASH)) {
            return false;
        }
        int offset = cursor.peek().offset();
        return offset == 0 || Character.isWhitespace(text.charAt(offset - 1)) || text.charAt(offset - 1) == ';';
    }

    private void skipComment() {
        while (!atEnd() && !cursor.isCurrent(BasicToken.T_NEW_LINE)) {
            cursor.next();
        }
    }
}
