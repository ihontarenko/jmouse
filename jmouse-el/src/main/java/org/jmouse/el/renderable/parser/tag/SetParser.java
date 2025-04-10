package org.jmouse.el.renderable.parser.tag;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.node.SetNode;

import static org.jmouse.el.lexer.BasicToken.*;
import static org.jmouse.el.renderable.lexer.TemplateToken.T_SET;

public class SetParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(T_SET);

        SetNode setNode = new SetNode();
        setNode.setVariable(cursor.ensure(T_IDENTIFIER).value());

        cursor.ensure(T_EQ);

        ExpressionNode value = (ExpressionNode) context.getParser(ExpressionParser.class).parse(cursor, context);
        setNode.setValue(value);

        return setNode;
    }

    @Override
    public String getName() {
        return "set";
    }

}
