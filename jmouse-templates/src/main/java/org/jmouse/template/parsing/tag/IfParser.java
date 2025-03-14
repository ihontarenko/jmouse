package org.jmouse.template.parsing.tag;

import org.jmouse.template.lexer.TemplateToken;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.Node;
import org.jmouse.template.node.RenderableNode;
import org.jmouse.template.parsing.Parser;
import org.jmouse.template.parsing.TagParser;
import org.jmouse.template.parsing.ParserContext;
import org.jmouse.template.parsing.parser.OperatorParser;

public class IfParser implements TagParser {

    @Override
    public RenderableNode parse(TokenCursor cursor, ParserContext context) {
        // consume 'if'
        cursor.ensure(TemplateToken.T_IF);

        Parser parser = context.getParser(OperatorParser.class);

        Node node = parser.parse(cursor, context);

        System.out.println(cursor.current());

        return null;
    }

    @Override
    public String getName() {
        return "if";
    }

}
