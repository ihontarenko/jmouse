package org.jmouse.el.renderable.parser.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.MapNode;
import org.jmouse.el.parser.MapParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.ScopeNode;
import org.jmouse.el.renderable.parser.TemplateParser;

import static org.jmouse.el.renderable.lexer.TemplateToken.*;

public class ScopeParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        TemplateParser templateParser = (TemplateParser) context.getParser(TemplateParser.class);
        ScopeNode      node           = new ScopeNode();

        cursor.ensure(TemplateToken.T_SCOPE);

        if (cursor.currentIf(TemplateToken.T_WITH)) {
            if (context.getParser(MapParser.class).parse(cursor, context) instanceof MapNode mapNode) {
                node.setWith(mapNode);
            }
        }

        cursor.ensure(T_CLOSE_EXPRESSION);

        Matcher<TokenCursor> stopper = CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_SCOPE);
        Node                 body    = templateParser.parse(cursor, context, stopper);

        node.setBody(body);

        // Ensure that the "endscope" tag is present.
        cursor.ensure(T_END_SCOPE);

        return node;
    }

    @Override
    public String getName() {
        return "scope";
    }

}
