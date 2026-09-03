package org.jmouse.validator.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Trivia;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a whole {@code .jmv} file — the trivia around the block as well as the block.
 *
 * <p>⚠️ This exists as a second class rather than as two more lines in {@link ValidationParser},
 * because leading comments, blank lines and whatever follows the closing brace are a question about a
 * <em>file</em>. {@link org.jmouse.el.language.parser.AbstractBlockParser} knows nothing about files
 * and should not: it ensures its keyword under the cursor, which is exactly right for a block and
 * refuses any document whose first line is a comment. jMP splits the same way.</p>
 *
 * <p>⚠️ Its {@code supports} stays {@code false}. This is the root a reader hands a whole file to, not
 * one more shape a statement might be, and offering it to dispatch would let a document nest inside
 * another — a file with two identities and nothing saying which is the outer one.</p>
 *
 * <p>⚠️ <strong>A file's header comment is kept.</strong> It is the one comment in a document that
 * belongs to no statement, and it is usually the one that explains why the document exists at all —
 * so it is collected here and attached to the document.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ValidationDocumentParser extends ExpressionParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        List<Trivia> header = new ArrayList<>();

        skipBlankSpace(cursor, header);

        Node document = context.getParser(ValidationParser.class).parse(cursor, context);

        document.addLeadingTrivia(header);
        parent.add(document);

        skipBlankSpace(cursor, new ArrayList<>());

        // ⚠️ Refused rather than ignored. A statement after the closing brace is somebody's rule, and
        // silently dropping it means a document that validates less than its author believes it does —
        // which is the one failure mode a validation language must never have.
        if (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_EOL)) {
            throw new JmvSyntaxException(cursor,
                    ("a file declaring 'validation' declares nothing beside it, and '%s' is outside "
                     + "the block; move it inside").formatted(cursor.current().value()));
        }
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return false;
    }

    /**
     * Steps over newlines, semicolons and {@code #} comments, keeping the comments.
     *
     * <p>Only ever called at the two edges of a file. Inside the block, {@code StatementsParser} does
     * the same job between statements, and a check line reads its own newline as a terminator — a
     * helper applied everywhere would let one line run into the next.</p>
     *
     * @param cursor the cursor to advance
     * @param into   where to record the comments
     */
    private static void skipBlankSpace(TokenCursor cursor, List<Trivia> into) {
        int written = 0;

        while (cursor.hasNext()) {
            if (cursor.consumeIf(BasicToken.T_SOL, BasicToken.T_NEW_LINE, BasicToken.T_SEMICOLON)) {
                continue;
            }

            if (!cursor.isCurrent(BasicToken.T_HASH)) {
                break;
            }

            // ⚠️ By line number, never by counting newline tokens — the lexer hands back one for a run
            // of them, so a blank line does not exist in the token stream at all.
            if (written > 0 && cursor.current().lineNumber() - written > 1) {
                into.add(Trivia.blank());
            }

            into.add(Trivia.comment(readCommentLine(cursor)));
            written = cursor.hasNext() ? cursor.current().lineNumber() : written;
        }

        if (written > 0 && cursor.hasNext() && cursor.current().lineNumber() - written > 1) {
            into.add(Trivia.blank());
        }
    }

    /**
     * Consumes a {@code #} line and hands back what it said, sliced from the source rather than
     * rebuilt from its tokens.
     *
     * @param cursor the cursor, positioned on the hash
     * @return the comment as typed
     */
    private static String readCommentLine(TokenCursor cursor) {
        Token first = cursor.current();
        Token last  = first;

        while (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_NEW_LINE, BasicToken.T_EOL)) {
            last = cursor.next();
        }

        return SourceReading.text(cursor, first, last);
    }
}
