package org.jmouse.query.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;

/**
 * One {@code order} clause, read on its own — what {@code ?jmq:order=} carries.
 *
 * <h2>⚠️ A third entry point, and it exists so that a URL never assembles a document</h2>
 *
 * <p>A sort arrives beside a filter and is not one expression: {@code entry[quantity] | int desc,
 * created asc} is two keys and two directions. The obvious implementation — wrap it in
 * {@code view "…" on x { order … }} and parse that — is <strong>string assembly of a query out of a
 * URL</strong>: a sort containing a brace would close the block early and add clauses of its own. It
 * would reach no data the schema does not describe, but it would let a caller restructure the query,
 * and a language whose safety rests on "they probably will not type a brace" has none.</p>
 *
 * <p>So the clause is read directly, by the same code a document's {@code order} is read by. A brace
 * here is what it should be: a syntax error, named and refused.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class OrderParser extends ExpressionParser {

    /**
     * Reads the sort keys, without the {@code order} keyword in front of them.
     *
     * @param cursor  positioned at the first key
     * @param parent  where the clause is added
     * @param context the parser context
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Documents.skipBlankSpace(cursor);

        parent.add(ClauseParser.parseKeys(cursor, context));
    }
}
