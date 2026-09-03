package org.jmouse.script.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AutodetectFirstParser;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.script.el.ScriptParseException;
import org.jmouse.script.el.SourceReader;
import org.jmouse.script.el.node.ScriptDocumentNode;

/**
 * Reads a whole {@code .jms} file — every declaration in it, to the last one.
 *
 * <p>⚠️ <strong>This exists because an expression parser reads one expression.</strong> A file is a
 * sequence of top-level declarations, and asked for an expression the language returns the first of
 * them and stops — the {@code behaviour} block below a {@code script} block is then not refused, not
 * reported, simply absent. In a file whose whole job is to say what happens when, a declaration that
 * quietly is not there is the worst failure available.</p>
 *
 * <p>So this is the root parser rather than one more registered alternative: it never appears in
 * dispatch ({@code supports} stays {@code false}) and is reached only as the entry point the evaluator
 * hands the file to.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptDocumentParser extends ExpressionParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        AutodetectFirstParser parser   = (AutodetectFirstParser) context.getParser(AutodetectFirstParser.class);
        ScriptDocumentNode    document = new ScriptDocumentNode(null);

        document.setSpan(SourceReader.span(cursor));

        skipBlankSpace(cursor);

        while (hasMore(cursor)) {
            int        position    = cursor.position();
            Expression declaration = (Expression) parser.parse(cursor, context);

            if (declaration == null || cursor.position() == position) {
                throw new ScriptParseException(
                        SourceReader.at(cursor),
                        ("'%s' is not a script declaration; a file holds 'include', 'script' and "
                                + "'behaviour', and nothing else at its top level")
                                .formatted(cursor.current().value())
                );
            }

            document.addDeclaration(declaration);

            skipBlankSpace(cursor);
        }

        parent.add(document);
    }

    /**
     * Whether anything but the end of the file is left.
     *
     * @param cursor the cursor to inspect
     * @return {@code true} while a declaration may still follow
     */
    private boolean hasMore(TokenCursor cursor) {
        return cursor.hasNext() && !cursor.isCurrent(BasicToken.T_EOL);
    }

    /**
     * Consumes the separators and comment lines between two declarations.
     *
     * @param cursor the cursor to advance
     */
    private void skipBlankSpace(TokenCursor cursor) {
        while (cursor.consumeIf(BasicToken.T_NEW_LINE, BasicToken.T_SEMICOLON) || skipComment(cursor)) {
            // a file may hold any amount of nothing between two declarations
        }
    }

    /**
     * Consumes a {@code #} comment through to the end of its line.
     *
     * @param cursor the cursor to advance
     * @return {@code true} when a comment was there
     */
    private boolean skipComment(TokenCursor cursor) {
        if (!cursor.isCurrent(BasicToken.T_HASH)) {
            return false;
        }

        while (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_NEW_LINE, BasicToken.T_EOL)) {
            cursor.next();
        }

        return true;
    }

}
