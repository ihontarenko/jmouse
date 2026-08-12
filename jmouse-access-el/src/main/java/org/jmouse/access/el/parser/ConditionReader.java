package org.jmouse.access.el.parser;

import org.jmouse.access.el.SourceReader;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;

/**
 * Reads what follows {@code when}, and returns it <strong>exactly as it was typed</strong>.
 *
 * <p>Extracted so that a grant and a role assignment read a condition the same way. They are two
 * statements in two blocks with two parsers, and the reading is subtle enough that two copies would
 * drift — at which point one of them silently truncates and the other does not.
 *
 * <h2>⚠️ It is not parsed here, and that is the fix rather than a shortcut</h2>
 *
 * <p>This used to run the full expression parser over the cursor to find where the condition ends —
 * and the result was thrown away, because the text is sliced out of the source either way and
 * {@code ExpressionConditionCompiler} re-lexes and compiles it later with a lexer of its own. A whole
 * parse was being paid for a delimiter.
 *
 * <p>Worse, it was the <em>wrong</em> parse. These tokens come out of
 * {@link org.jmouse.access.el.lexer.AccessRecognizer}, which turns this grammar's keywords into
 * keywords <em>wherever they appear</em> — so an ordinary condition naming the caller, a role or a
 * plan arrived at the expression parser as {@code T_SUBJECT}, {@code T_ROLE}, {@code T_PLAN}, and
 * {@code PrimaryExpressionParser} takes only {@code T_IDENTIFIER}. The file then failed to
 * <em>parse</em> over a word that is not part of its grammar at that position at all, with a message
 * about a token nobody wrote.
 *
 * <p>So the end of a condition is found the way every other statement in this grammar finds it: by
 * running to the end of the line. A statement is one line, the restricted dialect has no braces and
 * no separators, and nothing it may contain can span a newline — so there is no expression this reads
 * too little of, and no keyword left to trip over.
 */
public final class ConditionReader {

    /**
     * Where a condition stops: the end of the line, or the brace that closes the block it is in.
     *
     * <p>The closing brace is here because a statement may be the last line before it with nothing
     * between them, and because the condition dialect has no braces of its own — so one appearing
     * here can only be the block's.
     */
    private static final Token.Type[] TERMINATORS = {
            BasicToken.T_NEW_LINE, BasicToken.T_EOL, BasicToken.T_SEMICOLON, BasicToken.T_CLOSE_CURLY,
    };

    private ConditionReader() {
    }

    /**
     * The condition source, verbatim.
     *
     * @param cursor positioned on the first token after {@code when}
     */
    public static String read(TokenCursor cursor) {
        if (cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
            return readBraced(cursor);
        }

        Token first = cursor.current();
        Token last  = first;

        while (cursor.hasNext() && !cursor.isCurrent(TERMINATORS)) {
            last = cursor.next();
        }

        return SourceReader.text(cursor, first, last);
    }

    /**
     * Reads {@code when { … }} — the same condition, allowed to breathe across lines.
     *
     * <p>⚠️ <strong>It exists because the one-line form silently truncates.</strong> A condition runs
     * to the end of its line, so a rule broken across three for readability parses as a rule ending
     * after the first — and what is left is a <em>weaker</em> rule that still loads, still binds and
     * still reads like the one somebody wrote. Every failure this grammar is careful about is of that
     * shape, and this was the last one it had.
     *
     * <p>The braces are a delimiter and nothing else: what comes back is the text between them, with
     * newlines collapsed, and it compiles through exactly the same restricted dialect. There is no
     * second syntax to learn — {@code when x and y} and {@code when { x and y }} produce the same
     * condition and the same {@link org.jmouse.access.spi.GrantCondition#source()}.
     *
     * <p>⚠️ Nesting is counted rather than assumed. The dialect has no braces of its own today, so the
     * count is always one — but a matched pair is what a reader expects a brace to mean, and a parser
     * that stopped at the first {@code &#125;} would be one dialect change away from truncating again.
     */
    private static String readBraced(TokenCursor cursor) {
        cursor.ensure(BasicToken.T_OPEN_CURLY);

        if (cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            // An empty `when { }` is a statement that says it is conditional and states no condition.
            // Answered as empty rather than as the brace itself, so the compiler's own message —
            // "a condition cannot be empty" — is what somebody reads.
            cursor.ensure(BasicToken.T_CLOSE_CURLY);
            return "";
        }

        Token first = cursor.current();
        Token last  = first;
        int   depth = 1;

        while (cursor.hasNext()) {
            if (cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
                depth++;
            } else if (cursor.isCurrent(BasicToken.T_CLOSE_CURLY) && --depth == 0) {
                break;
            }

            last = cursor.next();
        }

        cursor.ensure(BasicToken.T_CLOSE_CURLY);

        return asOneLine(SourceReader.text(cursor, first, last));
    }

    /**
     * Collapses the newlines and indentation a braced condition was laid out with — and nothing else.
     *
     * <p>⚠️ <strong>Outside quotes only.</strong> A blanket {@code replaceAll("\\s+", " ")} also
     * rewrites what is <em>inside</em> a string literal, so {@code when { name == 'John  Doe' }} binds
     * a rule about {@code 'John Doe'}: it loads, it reads correctly, and it never matches. A condition
     * that quietly means something other than what is written is the failure this whole module is
     * built to avoid.
     *
     * <p>A literal cannot span a line in this dialect, so nothing is lost by leaving its insides
     * exactly as they were typed.
     */
    private static String asOneLine(String source) {
        StringBuilder collapsed     = new StringBuilder(source.length());
        char          quote         = 0;
        boolean       lastWasSpace  = false;

        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);

            if (quote != 0) {
                collapsed.append(character);
                if (character == quote) {
                    quote = 0;
                }
                continue;
            }

            if (character == '\'' || character == '"') {
                quote        = character;
                lastWasSpace = false;
                collapsed.append(character);
                continue;
            }

            if (Character.isWhitespace(character)) {
                if (!lastWasSpace) {
                    collapsed.append(' ');
                    lastWasSpace = true;
                }
                continue;
            }

            lastWasSpace = false;
            collapsed.append(character);
        }

        return collapsed.toString().trim();
    }
}
