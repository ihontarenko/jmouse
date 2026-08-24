package org.jmouse.query.el.parser;

import org.jmouse.core.MimeParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.query.el.lexer.QueryToken;
import org.jmouse.query.el.node.ViewNode;

/**
 * Parses {@code view "name" on target { … }}.
 *
 * <pre>
 *   view "Мої косарки" on inventory {
 *     where   entry[component_name] is contains("кос")
 *     order   entry[quantity] | int asc
 *     columns component_name, quantity, location
 *   }
 * </pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ViewParser extends AbstractParser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        cursor.ensure(QueryToken.T_VIEW);

        ViewNode view = new ViewNode();

        // ⚠️ Unquoted here. The token carries the quotes as they were typed, and they are how the title
        // was written rather than part of what it says — a view whose stored title includes its own
        // quotation marks shows them on every screen that displays it.
        view.setTitle(MimeParser.unquote(cursor.ensure(BasicToken.T_STRING).value()));

        cursor.ensure(QueryToken.T_ON);

        // ⚠️ An identifier and nothing more. Resolving what it names — a section, a purpose, a table —
        // belongs to the product that holds the data, and a language that resolved it could serve only
        // the one product whose answer it had baked in.
        view.setTarget(cursor.ensure(BasicToken.T_IDENTIFIER, BasicToken.T_STRING).value());

        cursor.ensure(BasicToken.T_OPEN_CURLY);

        while (!cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            Documents.skipBlankSpace(cursor);

            if (cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
                break;
            }

            Documents.refuseReserved(cursor);

            Token opening = cursor.peek();

            view.addClause(ClauseParser.parse(cursor, context), opening);
        }

        cursor.ensure(BasicToken.T_CLOSE_CURLY);

        parent.add(view);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return cursor.isCurrent(QueryToken.T_VIEW);
    }
}
