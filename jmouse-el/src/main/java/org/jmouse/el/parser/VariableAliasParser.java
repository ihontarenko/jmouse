package org.jmouse.el.parser;

import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.VariableAliasNode;

import static org.jmouse.el.lexer.BasicToken.*;

public class VariableAliasParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        VariableAliasNode node    = new VariableAliasNode();
        StringBuilder     builder = new StringBuilder();

        cursor.ensure(T_DOLLAR);
        node.setAliasName(cursor.ensure(T_IDENTIFIER).value());
        cursor.ensure(T_COLON);

        do {
            builder.append(cursor.current().value());

            if (cursor.isNext(T_IDENTIFIER, T_MINUS, T_NUMERIC)) {
                cursor.next();
                continue;
            }

            break;
        } while (true);

        node.setVariableName(builder.toString());

        parent.add(node);
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return CursorMatcher.variableAlias().matches(cursor);
    }
}
