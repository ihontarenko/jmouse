package org.jmouse.el.renderable.parser.tag;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.LiteralNode;
import org.jmouse.el.node.expression.NameSetNode;
import org.jmouse.el.node.expression.literal.StringLiteralNode;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.parser.sub.NamesParser;
import org.jmouse.el.renderable.node.UseNode;

import static org.jmouse.el.renderable.lexer.TemplateToken.*;

public class UseParser implements TagParser {

    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        cursor.ensure(T_USE);

        Node path = context.getParser(ExpressionParser.class).parse(cursor, context);

        cursor.ensure(T_GET);

        Token   token = cursor.ensure(T_MACRO, T_BLOCK);
        UseNode use   = new UseNode();

        use.setPath((ExpressionNode) path);
        use.setType(token.type());

        if (cursor.currentIf(T_AS)) {
            LiteralNode<String> alias = new StringLiteralNode(cursor.ensure(BasicToken.T_IDENTIFIER).value());
            use.setAlias(alias);
        } else if (cursor.currentIf(T_IMPORT)) {
            NameSetNode names = (NameSetNode) context.getParser(NamesParser.class).parse(cursor, context);
            use.setNames(names);
        }

        return use;
    }

    @Override
    public String getName() {
        return "use";
    }

}
