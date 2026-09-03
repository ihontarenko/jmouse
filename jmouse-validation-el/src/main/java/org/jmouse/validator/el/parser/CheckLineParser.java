package org.jmouse.validator.el.parser;

import org.jmouse.core.Priority;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.validator.el.lexer.JmvToken;
import org.jmouse.validator.el.node.CheckLineNode;
import org.jmouse.validator.el.node.CheckNode;

/**
 * Reads {@code field : check[, check]…} and any continuation line carrying the line's message.
 *
 * <p>The default statement of the language: what a line is when it is nothing else. That is why it
 * sorts last in {@link JmvParserPriority} — a {@code when} whose guard opens with an identifier would
 * otherwise be read as a field called {@code when}.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(JmvParserPriority.CHECK_LINE)
public class CheckLineParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        CheckLineNode line = new CheckLineNode();

        line.setField(cursor.ensure(JmvToken.nameTokens()).value());
        cursor.ensure(BasicToken.T_COLON);

        do {
            line.addCheck(readCheck(cursor));
        } while (continues(cursor));

        readLineMessage(cursor, line);

        parent.add(line);
    }

    /**
     * Whether another check follows — and lets it stand on the next line.
     *
     * <p>⚠️ <strong>A comma is allowed to end a physical line.</strong> Four checks with their own
     * messages do not fit on one, and a language that made an author choose between wrapping and saying
     * what is wrong would get one of the two given up. It costs no ambiguity: a comma at the end of a
     * check list means another check is coming, so the newline after it cannot be terminating
     * anything.</p>
     *
     * @param cursor the cursor, positioned after a check
     * @return whether the list goes on
     */
    private boolean continues(TokenCursor cursor) {
        if (!cursor.consumeIf(BasicToken.T_COMMA)) {
            return false;
        }

        while (cursor.consumeIf(BasicToken.T_NEW_LINE, BasicToken.T_EOL)) {
            // Nothing to do — consumeIf has already advanced past the separator.
        }

        return true;
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.opensCheckLine(cursor);
    }

    /**
     * Reads one check: a name, optional arguments, an optional {@code stop}, an optional message.
     *
     * @param cursor the cursor, positioned on the check's name
     * @return the check
     */
    private CheckNode readCheck(TokenCursor cursor) {
        CheckNode check = new CheckNode();

        check.setName(cursor.ensure(JmvToken.nameTokens()).value());

        if (cursor.consumeIf(BasicToken.T_OPEN_PAREN)) {
            readArguments(cursor, check);
            cursor.ensure(BasicToken.T_CLOSE_PAREN);
        }

        check.setStop(cursor.consumeIf(JmvToken.T_STOP));

        if (cursor.consumeIf(BasicToken.T_COLON)) {
            check.setMessage(ExpressionSlice.message(cursor));
        }

        return check;
    }

    /**
     * Reads what stands between a check's parentheses — values by position, values by name, or both.
     *
     * <p>⚠️ <strong>Not {@code ArgumentsParser} or {@code KeyValueParser}, and the reason is the one
     * that makes {@link ExpressionSlice} exist.</strong> Both of those build expression trees with this
     * cursor, whose lexer turns {@code stop}, {@code gate} and {@code always} into keywords wherever
     * they appear — so {@code min(stop_after)} would fail to parse over a field name the product was
     * entitled to choose. Every expression in a {@code .jmv} is kept as written and compiled later by a
     * plain {@code ExpressionLanguage}, and an argument is an expression.</p>
     *
     * <p>A named argument is a name followed by a colon. Nothing else in this position is, so one token
     * of lookahead settles it without a backtrack.</p>
     *
     * @param cursor the cursor, positioned just after the opening parenthesis
     * @param check  the check being filled
     */
    private void readArguments(TokenCursor cursor, CheckNode check) {
        if (cursor.isCurrent(BasicToken.T_CLOSE_PAREN)) {
            return;
        }

        do {
            if (cursor.isCurrent(JmvToken.nameTokens()) && cursor.isNext(BasicToken.T_COLON)) {
                String key = cursor.next().value();

                cursor.ensure(BasicToken.T_COLON);

                if (check.addNamed(key, ExpressionSlice.argument(cursor)) != null) {
                    throw new JmvSyntaxException(cursor,
                            ("'%s' is given twice to '%s' — a check where an argument means whichever "
                             + "one came last is a check nobody can read")
                                    .formatted(key, check.getName()));
                }

                continue;
            }

            check.addPositional(ExpressionSlice.argument(cursor));
        } while (cursor.consumeIf(BasicToken.T_COMMA));
    }

    /**
     * Reads a continuation line opening with {@code :}, if there is one.
     *
     * <p>A colon at the start of a line can only be this: every other line opens with a name. That is
     * what makes the form unambiguous, and it is why an inline line-message does not exist — written
     * inline it would be indistinguishable from a message on the last check.</p>
     *
     * <p>⚠️ <strong>An aside at the end of the checks is stepped over, and kept.</strong> A wrapped line
     * has two ends and somebody may write on either:</p>
     *
     * <pre>{@code
     * part_number : required stop, notBlank, size(3, 32)   # the common failure
     *             : 'A part number looks like AB-1234'
     * }</pre>
     *
     * <p>Refusing to step over that comment made the pair unparseable — the message was left to be read
     * as the next statement, which is a colon where a field name belongs. And dropping it would move
     * somebody's aside a line down on every save, which is worse than losing it.</p>
     *
     * @param cursor the cursor, positioned at the end of the check list
     * @param line   the line being filled
     */
    private void readLineMessage(TokenCursor cursor, CheckLineNode line) {
        TokenCursor.Savepoint savepoint = cursor.savepoint();
        String                aside     = cursor.isCurrent(BasicToken.T_HASH)
                ? ExpressionSlice.comment(cursor)
                : null;

        while (cursor.consumeIf(BasicToken.T_NEW_LINE, BasicToken.T_EOL, BasicToken.T_SEMICOLON)) {
            // Nothing to do — consumeIf has already advanced past the separator.
        }

        if (!cursor.isCurrent(BasicToken.T_COLON)) {
            // ⚠️ Wound all the way back, aside included. With no continuation the comment is an ordinary
            // trailing one and belongs to whoever reads statements, not to this parser.
            cursor.restore(savepoint);

            return;
        }

        cursor.ensure(BasicToken.T_COLON);
        line.setMessage(ExpressionSlice.message(cursor));
        line.setChecksNote(aside);
    }
}
