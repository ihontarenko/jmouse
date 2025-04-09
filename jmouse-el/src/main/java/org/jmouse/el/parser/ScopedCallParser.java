package org.jmouse.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.node.expression.ScopedCallNode;

public class ScopedCallParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        FunctionParser parser = (FunctionParser) context.getParser(FunctionParser.class);
        Token          scope  = cursor.ensure(BasicToken.T_IDENTIFIER);

        cursor.ensure(BasicToken.T_DOT);

        FunctionNode   function = (FunctionNode) parser.parse(cursor, context);
        ScopedCallNode node     = new ScopedCallNode(function.getName());

        node.setScope(scope.value());
        node.setArguments(function.getArguments());

        parent.add(node);
    }

}
