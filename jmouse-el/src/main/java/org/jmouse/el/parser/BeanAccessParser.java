package org.jmouse.el.parser;

import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.BeanAccessNode;
import org.jmouse.el.node.expression.literal.NullLiteralNode;
import org.jmouse.el.parser.sub.ArgumentsParser;

import static org.jmouse.el.lexer.BasicToken.*;
import static org.jmouse.el.node.expression.BeanAccessNode.AccessType.*;

public class BeanAccessParser implements Parser {

    private final CursorMatcher.BeanMatcher matcher = (CursorMatcher.BeanMatcher) CursorMatcher.bean();

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        BeanAccessNode            node       = new BeanAccessNode();
        BeanAccessNode.AccessType accessType = getAccessType(cursor);
        node.setType(accessType);

        cursor.ensure(BasicToken.T_AT);

        node.setBean(
                cursor.ensure(T_IDENTIFIER).value()
        );

        switch (accessType) {
            case METHOD_CALL:
                cursor.ensure(T_DOT);

                Expression  arguments   = new NullLiteralNode();
                String      name        = cursor.ensure(T_IDENTIFIER).value();

                if (!cursor.matchesSequence(T_OPEN_PAREN, T_CLOSE_PAREN)) {
                    arguments = (Expression) context.getParser(ArgumentsParser.class).parse(cursor, context);
                } else {
                    cursor.ensure(T_OPEN_PAREN);
                    cursor.ensure(T_CLOSE_PAREN);
                }

                node.setAction(new BeanAccessNode.MethodCall(name, arguments));
                break;
            case FIELD_ACCESS: {
                cursor.ensure(T_COLON);
                cursor.ensure(T_DOLLAR);
                node.setAction(new BeanAccessNode.FieldAccess(cursor.ensure(T_IDENTIFIER).value()));
                break;
            }
            case CONSTANT_ACCESS:
                cursor.ensure(T_HASH);
                node.setAction(new BeanAccessNode.ConstantAccess(cursor.ensure(T_IDENTIFIER).value()));
                break;
            default:
        }

        parent.add(node);
    }

    private BeanAccessNode.AccessType getAccessType(TokenCursor cursor) {
        return matcher.methodCall(cursor) ? METHOD_CALL : matcher.constantAccess(cursor) ? CONSTANT_ACCESS : FIELD_ACCESS;
    }

    @Override
    public boolean supports(TokenCursor cursor) {
        return matcher.matches(cursor);
    }

}
