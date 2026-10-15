package org.jmouse.formula.el;

import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.FunctionParser;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.ParseException;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.PrimaryExpressionParser;
import org.jmouse.el.parser.PropertyParser;

import static org.jmouse.el.lexer.BasicToken.T_CLOSE_PAREN;
import static org.jmouse.el.lexer.BasicToken.T_IDENTIFIER;
import static org.jmouse.el.lexer.BasicToken.T_NEW_LINE;
import static org.jmouse.el.lexer.BasicToken.T_OPEN_PAREN;

/** jMF's primary expressions: numbers, names, calls and groups, without jME's lambda ambiguity for (x). */
public final class FormulaPrimaryExpressionParser extends PrimaryExpressionParser {
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        if (CursorMatcher.function().matches(cursor)) {
            parent.add(context.getParser(FunctionParser.class).parse(cursor, context));
        } else if (CursorMatcher.literal().matches(cursor)) {
            parent.add(context.getParser(LiteralParser.class).parse(cursor, context));
        } else if (cursor.isCurrent(T_IDENTIFIER)) {
            parent.add(context.getParser(PropertyParser.class).parse(cursor, context));
        } else if (cursor.consumeIf(T_OPEN_PAREN)) {
            parent.add(context.getParser(ExpressionParser.class).parse(cursor, context));
            cursor.ensure(T_CLOSE_PAREN);
        } else if (cursor.consumeIf(T_NEW_LINE)) {
            parse(cursor, parent, context);
        } else {
            throw new ParseException("Unexpected token " + cursor.peek() + " at " + cursor.position());
        }
    }
}
