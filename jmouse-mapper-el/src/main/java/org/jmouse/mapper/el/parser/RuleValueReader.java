package org.jmouse.mapper.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.mapper.el.lexer.JmmToken;

/**
 * Reads what stands to the right of a rule's {@code :} — and returns it <strong>exactly as it was
 * typed</strong>, without parsing it. 📐
 *
 * <h2>⚠️ Not parsing it here is the whole point, not a shortcut</h2>
 *
 * <p>The obvious implementation hands the cursor to the expression parser and keeps the tree. It does
 * not work, and the way it fails is quiet enough to be worth spelling out.</p>
 *
 * <p>{@link org.jmouse.mapper.el.lexer.JmmRecognizer} turns this language's words into keywords
 * <em>wherever they appear</em> — that is what a recognizer is for. So {@code source.total} arrives as
 * {@code T_SOURCE}, {@code .}, {@code total}; a source property called {@code from}, {@code when} or
 * {@code target} arrives as its keyword too. The expression parser's primary rule accepts
 * {@code T_IDENTIFIER}, so every one of those fails to <em>parse</em> — over a word the file was
 * entirely entitled to write, with a message about a token nobody typed.</p>
 *
 * <p>⚠️ And it cannot be fixed by widening the expression parser: the same word has to be a keyword in
 * {@code refuse source before} and an ordinary name in {@code source.total}, three lines apart in one
 * file. A lexer cannot know which, and a parser that guessed would be wrong half the time.</p>
 *
 * <p>So the value is <strong>sliced out of the source</strong> and compiled later by a plain
 * {@link org.jmouse.el.ExpressionLanguage}, whose lexer has never heard of {@code target} or
 * {@code from} and reads them as the identifiers they are. This is the answer the policy language
 * already arrived at for its conditions, for exactly these reasons.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class RuleValueReader {

    /**
     * Where a value stops.
     *
     * <p>A rule is one line. The closing brace is here because a rule may be the last one before it
     * with nothing between them; {@code when} is here because it is the one keyword that may follow a
     * value on its own line, and a reader stopping only at the newline would swallow the condition into
     * the value — where it would compile, evaluate to something, and be written to the property.</p>
     *
     * <p>⚠️ {@code #} is here for the same reason {@code when} is: a trailing comment would otherwise be
     * sliced into the expression and handed to a compiler that has no idea what it is. It is safe as a
     * terminator because a {@code #} inside a string literal is part of that literal's token and never
     * arrives as {@code T_HASH} — the lexer has already decided.</p>
     */
    private static final Token.Type[] TERMINATORS = {
            BasicToken.T_NEW_LINE,
            BasicToken.T_EOL,
            BasicToken.T_SEMICOLON,
            BasicToken.T_CLOSE_CURLY,
            BasicToken.T_HASH,
            JmmToken.T_WHEN
    };

    private RuleValueReader() {
    }

    /**
     * Reads a value from the cursor's position to the end of its line.
     *
     * @param cursor the cursor, positioned on the first token of the value
     * @return what was written, verbatim and trimmed
     * @throws JmmSyntaxException when there is no value at all
     */
    public static String read(TokenCursor cursor) {
        return slice(cursor, "a rule needs a value after ':'");
    }

    /**
     * Reads the condition after {@code when}, the same way and for the same reasons.
     *
     * @param cursor the cursor, positioned on the {@code when}
     * @return the condition verbatim, or {@code null} when the rule carries none
     */
    public static String readCondition(TokenCursor cursor) {
        if (!cursor.isCurrent(JmmToken.T_WHEN)) {
            return null;
        }

        cursor.ensure(JmmToken.T_WHEN);

        return slice(cursor, "'when' needs a condition after it");
    }

    /**
     * Cuts everything from here to the end of the line out of the source, verbatim.
     *
     * <p>⚠️ One implementation, because a value and a condition differ only in what precedes them and in
     * what to say when there is nothing there. They were written out twice, which meant the next
     * terminator anybody adds would have had to be added twice — and the day it is added once is the day
     * a trailing comment ends up inside a condition and not inside a value.</p>
     *
     * @param cursor  the cursor, positioned on the first token to keep
     * @param missing what to say when the line ends before anything was written
     * @return the text as it was typed
     * @throws JmmSyntaxException when there is nothing to read
     */
    private static String slice(TokenCursor cursor, String missing) {
        Token first = cursor.current();

        if (first == null || isTerminator(cursor)) {
            throw new JmmSyntaxException(cursor, missing);
        }

        Token last = first;

        while (cursor.hasNext() && !isTerminator(cursor)) {
            last = cursor.next();
        }

        return SourceReading.text(cursor, first, last);
    }

    /**
     * Whether the cursor sits on something that ends a value.
     *
     * @param cursor the cursor to test
     * @return {@code true} when the value is over
     */
    private static boolean isTerminator(TokenCursor cursor) {
        return cursor.isCurrent(TERMINATORS);
    }
}
