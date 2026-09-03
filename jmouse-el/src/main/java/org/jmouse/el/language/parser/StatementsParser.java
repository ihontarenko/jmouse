package org.jmouse.el.language.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Trivia;
import org.jmouse.el.parser.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a braced body of statements, and keeps what a person wrote around them.
 *
 * <h2>⚠️ Trivia is collected, not skipped</h2>
 *
 * <p>This used to consume a {@code #} line and move on, which is right for a parser and quietly
 * destructive for anything that renders a tree back into source: a builder opens a file, saves it, and
 * every explanation somebody wrote above their rules is gone with no diff to notice it in. Comments and
 * blank lines are now attached to the statement they were written above — see {@link Trivia}.</p>
 *
 * <p>⚠️ <strong>A trailing comment belongs to the line it sits on, not to the next one.</strong>
 * {@code total : price * quantity   # net of tax} explains the rule it follows; carried down to the
 * statement below it, it would be re-attached to something it says nothing about — which is worse than
 * losing it, because it reads as though somebody meant it there.</p>
 *
 * <p>⚠️ Trivia left over at the closing brace is attached to the <strong>container</strong>, so a
 * writer can put a block's parting comment back where it was rather than dropping it for having no
 * statement to belong to.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class StatementsParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        open(cursor);

        AutodetectFirstParser parser    = (AutodetectFirstParser) context.getParser(AutodetectFirstParser.class);
        BasicNode             container = BasicNode.forToken(cursor.current());
        List<Trivia>          pending   = new ArrayList<>();

        collectTrivia(cursor, pending);

        while (!isClosed(cursor)) {
            int        position   = cursor.position();
            Expression expression = (Expression) parser.parse(cursor, context);

            if (expression == null) {
                Token current = cursor.current();
                throw new ParseException("Unexpected token %s at line: %d".formatted(
                        cursor.peek(), current.lineNumber()));
            }

            expression.addLeadingTrivia(pending);
            pending.clear();

            container.add(expression);

            if (cursor.position() == position) {
                throw new ParseException(
                        "Parser for JMP statement '%s' returned without consuming it at token position %d"
                                .formatted(cursor.current().value(), position)
                );
            }

            // ⚠️ Before any separator is skipped, so a comment still on this line is still on this line.
            Trivia trailing = readTrailingComment(cursor);

            // ⚠️ Set only where there is one. A statement parser that already knows its own trailing
            // comment — a wrapped line, whose aside sits on its FIRST end — must not have it cleared
            // here by a null.
            if (trailing != null) {
                expression.setTrailingTrivia(trailing);
            }

            collectTrivia(cursor, pending);
        }

        container.addLeadingTrivia(pending);
        parent.add(container);

        close(cursor);
    }

    /**
     * Consumes whatever opens the body — a brace, here.
     *
     * <p>⚠️ <strong>These three methods are the whole of what a dialect has to change to write bodies a
     * different way</strong>, and they are separate from {@link #parse} for one reason: everything
     * {@code parse} does between them — trivia, the no-progress guard, a trailing comment staying on
     * the line it was written on — is not about braces at all, and a language whose bodies end in a
     * word would otherwise have to copy all of it in order to say so.</p>
     *
     * @param cursor the cursor, positioned on the body's opening token
     */
    protected void open(TokenCursor cursor) {
        cursor.ensure(BasicToken.T_OPEN_CURLY);
    }

    /**
     * Whether the body has ended.
     *
     * @param cursor the cursor, positioned where the next statement would begin
     * @return {@code true} when what is there closes the body instead
     */
    protected boolean isClosed(TokenCursor cursor) {
        return cursor.isCurrent(BasicToken.T_CLOSE_CURLY);
    }

    /**
     * Consumes whatever closes the body.
     *
     * @param cursor the cursor, positioned on the body's closing token
     */
    protected void close(TokenCursor cursor) {
        cursor.ensure(BasicToken.T_CLOSE_CURLY);
    }

    /**
     * Steps over separators and comments, keeping what is worth keeping.
     *
     * <p>⚠️ Consecutive blank lines collapse to one. Two empty lines and five are the same gesture, and
     * a writer that reproduced the count exactly would turn a stray keypress into a permanent feature of
     * the file.</p>
     *
     * @param cursor the cursor to advance
     * @param into   where to record what was written
     */
    private void collectTrivia(TokenCursor cursor, List<Trivia> into) {
        int written = lineOf(cursor);

        while (cursor.hasNext()) {
            if (cursor.isCurrent(BasicToken.T_NEW_LINE, BasicToken.T_EOL, BasicToken.T_SEMICOLON)) {
                cursor.next();

                continue;
            }

            if (!cursor.isCurrent(BasicToken.T_HASH)) {
                break;
            }

            separate(cursor, written, into);
            into.add(Trivia.comment(readCommentLine(cursor)));

            written = lineOf(cursor);
        }

        separate(cursor, written, into);
    }

    /**
     * Records a blank line where one was left between what came before and what comes next.
     *
     * <p>⚠️ <strong>Measured by line number, not by counting newline tokens.</strong> The lexer hands
     * back one {@code T_NEW_LINE} for a run of them, so a blank line is invisible in the token stream —
     * counted tokens say "one break" whether somebody left one line or five. The gap between the line
     * something was written on and the line the next thing is written on is the only place the
     * information survives.</p>
     *
     * <p>However many lines were left, one blank is recorded. Two empty lines and five are the same
     * gesture, and reproducing the count exactly would make a stray keypress a permanent feature.</p>
     *
     * @param cursor  the cursor, positioned on what comes next
     * @param written the line the previous thing was written on
     * @param into    where to record it
     */
    private void separate(TokenCursor cursor, int written, List<Trivia> into) {
        if (lineOf(cursor) - written > 1) {
            into.add(Trivia.blank());
        }
    }

    /**
     * The line the cursor is on, or the previous one at the end of the source.
     *
     * @param cursor the cursor to ask
     * @return the 1-based line
     */
    private int lineOf(TokenCursor cursor) {
        Token token = cursor.current();

        return token == null ? 0 : token.lineNumber();
    }

    /**
     * Reads a comment sitting at the end of the statement just parsed, if there is one.
     *
     * @param cursor the cursor, positioned immediately after a statement
     * @return the comment, or {@code null} when the line ended without one
     */
    private Trivia readTrailingComment(TokenCursor cursor) {
        if (!cursor.isCurrent(BasicToken.T_HASH)) {
            return null;
        }

        return Trivia.comment(readCommentLine(cursor));
    }

    /**
     * Consumes a {@code #} line and hands back what it said.
     *
     * <p>⚠️ <strong>Sliced out of the source, never rebuilt from the tokens.</strong> A comment is
     * prose: its spacing, its punctuation and its em-dashes are the whole of it, and a version
     * reassembled token by token comes back with a space wherever the lexer saw a boundary — which is
     * not what anybody wrote, and is worse than losing the line outright because it looks deliberate.</p>
     *
     * @param cursor the cursor, positioned on the hash
     * @return the comment as typed, {@code #} included
     */
    private String readCommentLine(TokenCursor cursor) {
        Token first = cursor.current();
        Token last  = first;

        while (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_NEW_LINE, BasicToken.T_EOL)) {
            last = cursor.next();
        }

        return SourceReading.text(cursor, first, last);
    }
}
