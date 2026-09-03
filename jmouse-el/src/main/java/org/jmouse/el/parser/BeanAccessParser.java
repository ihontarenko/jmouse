package org.jmouse.el.parser;

import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.BeanAccessNode;
import org.jmouse.el.node.expression.literal.NullLiteralNode;
import org.jmouse.el.parser.sub.ArgumentsParser;
import org.jmouse.el.parser.sub.ParenthesesParser;

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

                String name = cursor.ensure(T_IDENTIFIER).value();

                // ⚠️ Through ParenthesesParser with ArgumentsParser as the next parser — exactly as
                // FunctionParser does it, and not by calling ArgumentsParser directly.
                //
                // ⚠️ Calling it directly is what this used to do, and it cannot read more than one
                // argument: ArgumentsParser does not consume the brackets, so the leading '(' was handed
                // to the expression parser, which read it as a parenthesised expression and then
                // demanded a ')' where the first comma was. `@bean.method('a')` worked and
                // `@bean.method('a', 30)` failed with "expected T_CLOSE_PAREN" — pointing at the comma,
                // and saying nothing about brackets nobody wrote wrong.
                //
                // ⚠️ It stayed invisible because the only dialect on this parser before jMS was `.jmp`,
                // which deliberately does not register BeanAccessParser at all.
                context.setOptions(ParserOptions.withNextParser(ArgumentsParser.class));
                Expression parsed = (Expression) context.getParser(ParenthesesParser.class).parse(cursor, context);
                context.clearOptions();

                // An empty argument list parses to nothing at all, and MethodCall evaluates whatever it
                // is given — so "no arguments" is a null literal rather than a null reference.
                node.setAction(new BeanAccessNode.MethodCall(name, parsed == null ? new NullLiteralNode() : parsed));
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
