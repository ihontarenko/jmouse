package org.jmouse.el.grammar;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.literal.QuantityLiteralNode;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;

/** {@code width x height}: two quantities and the {@code x} between them, ending where the caller's cursor ends. */
public class ExtentParser extends AbstractParser {

    private static final BoundedTokenCursor.Boundary BY = (token, next, depth) ->
            depth == 0 && token.type() != BasicToken.T_STRING && token.value().equals("x");

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        QuantityLiteralNode width = QuantityValueParser.read(new BoundedTokenCursor(cursor, BY), context);
        if (!cursor.checkValueAt(0, "x")) {
            throw GrammarRefusal.here(cursor, "a size is 'width x height'");
        }
        cursor.next();
        parent.add(new ExtentNode(width, QuantityValueParser.read(cursor, context)));
    }

    public static ExtentNode read(TokenCursor cursor, ParserContext context) {
        return (ExtentNode) context.getParser(ExtentParser.class).parse(cursor, context);
    }
}
