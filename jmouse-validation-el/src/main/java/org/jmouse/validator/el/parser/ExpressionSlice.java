package org.jmouse.validator.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;

import java.util.List;

/**
 * Cuts an expression out of the source <strong>exactly as it was typed</strong>, without parsing it. ✂️
 *
 * <h2>⚠️ Not parsing it here is the whole point, not a shortcut</h2>
 *
 * <p>The obvious implementation hands the cursor to the expression parser and keeps the tree. It does
 * not work, and it fails quietly enough to be worth spelling out.</p>
 *
 * <p>{@link org.jmouse.validator.el.lexer.JmvRecognizer} turns this language's words into keywords
 * <em>wherever they appear</em> — that is what a recognizer is for. So a guard reading
 * {@code stop_reason == 'none'} arrives with {@code stop} as {@code T_STOP}, and a record with a field
 * called {@code gate} or {@code always} arrives the same way. The expression parser's primary rule
 * accepts {@code T_IDENTIFIER}, so every one of those fails to <em>parse</em> — over a word the file was
 * entirely entitled to write, with a message about a token nobody typed.</p>
 *
 * <p>⚠️ And it cannot be fixed by widening the expression parser: the same word has to be a keyword in
 * {@code required stop} and an ordinary name in {@code stop_reason == 'none'}, on adjacent lines of one
 * file. A lexer cannot know which, and a parser that guessed would be wrong half the time.</p>
 *
 * <p>So the expression is sliced out and compiled later by a plain
 * {@link org.jmouse.el.ExpressionLanguage}, whose lexer has never heard of {@code gate} or {@code stop}
 * and reads them as the identifiers they are. Both the policy and the mapping languages arrived at this
 * answer before this one existed.</p>
 *
 * <h2>⚠️ Depth, because a terminator inside brackets is not a terminator</h2>
 *
 * <p>A comma ends an argument and ends a check — but {@code oneOf('a', 'b')} carries one that ends
 * neither, and {@code url(host: 'x')} carries a colon that is not a message. So the slice tracks how
 * deep it is inside parentheses, brackets and braces, and only a terminator at depth zero stops it.
 * Without that, every check taking more than one argument would be cut in half.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ExpressionSlice {

    /** What ends a check line, a message, or the last argument of a call. */
    private static final Token.Type[] END_OF_LINE = {
            BasicToken.T_NEW_LINE,
            BasicToken.T_EOL,
            BasicToken.T_SEMICOLON,
            BasicToken.T_CLOSE_CURLY,
            BasicToken.T_HASH
    };

    private static final List<Token.Type> OPENING = List.of(
            BasicToken.T_OPEN_PAREN, BasicToken.T_OPEN_BRACKET, BasicToken.T_OPEN_CURLY);

    private static final List<Token.Type> CLOSING = List.of(
            BasicToken.T_CLOSE_PAREN, BasicToken.T_CLOSE_BRACKET, BasicToken.T_CLOSE_CURLY);

    private ExpressionSlice() {
    }

    /**
     * Reads to the end of the line.
     *
     * @param cursor  the cursor, positioned on the first token to keep
     * @param missing what to say when there is nothing there
     * @return the text as it was typed
     * @throws JmvSyntaxException when the line ends before anything was written
     */
    public static String toEndOfLine(TokenCursor cursor, String missing) {
        return slice(cursor, missing);
    }

    /**
     * Reads one argument of a call — to the next {@code ,} or the closing {@code )} outside any bracket.
     *
     * <p>⚠️ The closing parenthesis is tested <em>before</em> it is consumed, which is what lets it end
     * the last argument without the depth going negative. A slice that decremented first would swallow
     * the {@code )} and leave the parser looking for one that had already gone past.</p>
     *
     * @param cursor the cursor, positioned on the first token of the argument
     * @return the argument as it was typed
     * @throws JmvSyntaxException when there is nothing to read
     */
    public static String argument(TokenCursor cursor) {
        return slice(cursor, "an argument cannot be empty — write the value or remove the comma",
                     BasicToken.T_COMMA, BasicToken.T_CLOSE_PAREN);
    }

    /**
     * Reads a message — to the next {@code ,} outside any bracket, or the end of the line.
     *
     * <p>⚠️ A colon does <strong>not</strong> end it, because a message is an expression and
     * {@code value is blank ? 'a' : 'b'} contains one. Nothing legal follows a message on the same line
     * except the comma introducing the next check, so the comma is the only stop it needs.</p>
     *
     * @param cursor the cursor, positioned on the first token of the message
     * @return the message as it was typed
     * @throws JmvSyntaxException when the {@code :} is there and the message is not
     */
    public static String message(TokenCursor cursor) {
        return slice(cursor, "a ':' introduces a message — write one or drop the colon",
                     BasicToken.T_COMMA);
    }

    /**
     * Reads a {@code #} comment through to the end of its line.
     *
     * <p>⚠️ Its own method rather than {@link #toEndOfLine}, because {@code #} is itself one of that
     * one's terminators — asking it to read a comment stops it before the first character.</p>
     *
     * @param cursor the cursor, positioned on the hash
     * @return the comment as typed, {@code #} included
     */
    public static String comment(TokenCursor cursor) {
        Token first = cursor.current();
        Token last  = first;

        while (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_NEW_LINE, BasicToken.T_EOL)) {
            last = cursor.next();
        }

        return SourceReading.text(cursor, first, last);
    }

    /**
     * Reads to the {@code :} that introduces a message.
     *
     * @param cursor  the cursor, positioned on the first token to keep
     * @param missing what to say when there is nothing there
     * @return the text as it was typed
     * @throws JmvSyntaxException when there is nothing to read
     */
    public static String toColon(TokenCursor cursor, String missing) {
        return slice(cursor, missing, BasicToken.T_COLON);
    }

    /**
     * Reads a guard, which runs to the {@code &#123;} opening its block.
     *
     * <p>⚠️ Stopping at a brace at depth zero, so a condition containing one — a map literal — is not
     * cut short. The block's own opening brace is the first one this slice ever sees at depth zero,
     * because a guard that opened a bracket and did not close it would not be an expression.</p>
     *
     * @param cursor the cursor, positioned on the first token of the condition
     * @return the condition as it was typed
     * @throws JmvSyntaxException when there is no condition
     */
    public static String toBlock(TokenCursor cursor) {
        Token first = cursor.current();

        if (first == null || cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
            throw new JmvSyntaxException(cursor, "'when' needs a condition before its block");
        }

        Token last  = first;
        int   depth = 0;

        while (cursor.hasNext()) {
            if (depth == 0 && cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
                break;
            }

            if (cursor.isCurrent(END_OF_LINE[0], END_OF_LINE[1])) {
                throw new JmvSyntaxException(cursor, "'when' needs a block after its condition");
            }

            depth = depthAfter(cursor, depth);
            last = cursor.next();
        }

        return SourceReading.text(cursor, first, last);
    }

    /**
     * Cuts from here to the first terminator standing outside any bracket.
     *
     * @param cursor     the cursor, positioned on the first token to keep
     * @param missing    what to say when there is nothing to read
     * @param additional terminators beyond the end of the line
     * @return the text as it was typed
     */
    private static String slice(TokenCursor cursor, String missing, Token.Type... additional) {
        Token first = cursor.current();

        if (first == null || isTerminator(cursor, 0, additional)) {
            throw new JmvSyntaxException(cursor, missing);
        }

        Token last  = first;
        int   depth = 0;

        while (cursor.hasNext() && !isTerminator(cursor, depth, additional)) {
            depth = depthAfter(cursor, depth);
            last = cursor.next();
        }

        return SourceReading.text(cursor, first, last);
    }

    /**
     * Whether the cursor sits on something that ends the slice.
     *
     * <p>An end-of-line terminator ends it whatever the depth: a line break inside an unclosed bracket
     * is a file that is already wrong, and running on to find the closing one turns a missing
     * parenthesis into a failure three constructions later.</p>
     */
    private static boolean isTerminator(TokenCursor cursor, int depth, Token.Type... additional) {
        if (cursor.isCurrent(END_OF_LINE)) {
            return true;
        }

        return depth == 0 && additional.length > 0 && cursor.isCurrent(additional);
    }

    /**
     * The depth once the token under the cursor has been consumed.
     *
     * @param cursor the cursor, positioned on the token about to be consumed
     * @param depth  how deep it is now
     * @return how deep it will be
     */
    private static int depthAfter(TokenCursor cursor, int depth) {
        Token.Type type = cursor.current().type();

        if (OPENING.contains(type)) {
            return depth + 1;
        }

        return CLOSING.contains(type) ? depth - 1 : depth;
    }
}
