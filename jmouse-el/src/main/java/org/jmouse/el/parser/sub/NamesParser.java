package org.jmouse.el.parser.sub;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.NameNode;
import org.jmouse.el.node.expression.NameSetNode;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.renderable.lexer.TemplateToken;

public class NamesParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        NameSetNode names = new NameSetNode();

        do {
            NameNode name  = new NameNode();

            name.setName(cursor.ensure(BasicToken.T_IDENTIFIER).value());

            if (cursor.currentIf(TemplateToken.T_AS)) {
                name.setAlias(cursor.ensure(BasicToken.T_IDENTIFIER).value());
            }

            names.add(name);
        } while (cursor.isCurrent(BasicToken.T_COMMA) && cursor.next() != null);

        parent.add(names);
    }

}
