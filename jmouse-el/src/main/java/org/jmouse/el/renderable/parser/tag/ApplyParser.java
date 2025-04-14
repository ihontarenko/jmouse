package org.jmouse.el.renderable.parser.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.FilterNode;
import org.jmouse.el.parser.FilterParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.ApplyNode;
import org.jmouse.el.renderable.parser.TemplateParser;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.el.lexer.BasicToken.T_VERTICAL_SLASH;
import static org.jmouse.el.renderable.lexer.TemplateToken.*;

public class ApplyParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {

        ApplyNode        node           = new ApplyNode();
        TemplateParser   templateParser = (TemplateParser) context.getParser(TemplateParser.class);
        Parser           parser         = context.getParser(FilterParser.class);
        List<FilterNode> chain          = new ArrayList<>();

        cursor.ensure(TemplateToken.T_APPLY);

        do {
            if (parser.parse(cursor, context) instanceof FilterNode filter) {
                chain.add(filter);
            }
        } while (cursor.currentIf(T_VERTICAL_SLASH));

        cursor.ensure(T_CLOSE_EXPRESSION);

        Matcher<TokenCursor> stopper = CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_APPLY);
        Node                 body    = templateParser.parse(cursor, context, stopper);

        node.setBody(body);
        node.setChain(chain);

        // Ensure that the "endapply" tag is present.
        cursor.ensure(T_END_APPLY);

        return node;
    }

    @Override
    public String getName() {
        return "apply";
    }

}
