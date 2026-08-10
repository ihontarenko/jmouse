package org.jmouse.access.el.parser;

import org.jmouse.access.el.CursorMatcher;
import org.jmouse.access.el.SourceReader;
import org.jmouse.access.el.node.PolicyNode;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AutodetectFirstParser;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParseException;
import org.jmouse.el.parser.ParserContext;

/**
 * Reads a whole {@code .jmp} file, in either of the two shapes one may be written in.
 *
 * <p>⚠️ <strong>This exists because an expression parser reads one expression.</strong> A file
 * written without the {@code policy "…" { … }} wrapper is a sequence of top-level declarations, and
 * asked for an expression the language returns the first of them and stops — the {@code subject}
 * block below a {@code role} block is then not refused, not reported, simply absent. In a file whose
 * whole job is to say who may do what, a statement that quietly is not there is the worst failure
 * available.</p>
 *
 * <p>So this is the root parser rather than one more registered alternative: it never appears in
 * dispatch ({@code supports} stays {@code false}) and is reached only as the entry point the
 * evaluator hands the file to.</p>
 *
 * <p>A wrapper-less file becomes a {@link PolicyNode} with no name of its own, and the loader names
 * it after the file it came from.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PolicyDocumentParser extends ExpressionParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        skipBlankSpace(cursor);

        if (CursorMatcher.policy().matches(cursor)) {
            parent.add(parseWrapped(cursor, context));
            return;
        }

        parent.add(parseBare(cursor, context));
    }

    /**
     * Reads a file that names itself — {@code policy "innoventa-bootstrap" { … }}.
     *
     * @param cursor  the cursor, positioned on the {@code policy} keyword
     * @param context the parser context
     * @return the policy the file declared
     * @throws ParseException when anything follows the closing brace
     */
    private Expression parseWrapped(TokenCursor cursor, ParserContext context) {
        Expression policy = (Expression) context.getParser(PolicyParser.class).parse(cursor, context);

        skipBlankSpace(cursor);

        if (hasMore(cursor)) {
            throw new ParseException(("a file declaring 'policy' declares nothing beside it, and '%s' at line %d "
                    + "is outside the block; move it inside, or drop the wrapper")
                    .formatted(cursor.current().value(), cursor.current().lineNumber()));
        }

        return policy;
    }

    /**
     * Reads a file that is a bare sequence of declarations, to the last one.
     *
     * @param cursor  the cursor, positioned on the first declaration
     * @param context the parser context
     * @return an unnamed policy holding everything the file declared
     */
    private Expression parseBare(TokenCursor cursor, ParserContext context) {
        AutodetectFirstParser parser   = (AutodetectFirstParser) context.getParser(AutodetectFirstParser.class);
        PolicyNode            document = new PolicyNode(null);

        document.setSpan(SourceReader.span(cursor));

        while (hasMore(cursor)) {
            int        position    = cursor.position();
            Expression declaration = (Expression) parser.parse(cursor, context);

            if (declaration == null || cursor.position() == position) {
                throw new ParseException("'%s' at line %d is not a policy declaration".formatted(
                        cursor.current().value(), cursor.current().lineNumber()));
            }

            document.addExpression(declaration);

            skipBlankSpace(cursor);
        }

        return document;
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
