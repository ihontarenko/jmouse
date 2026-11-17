package org.jmouse.json.syntax;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.json.node.JsonArrayNode;

/**
 * 🧾 A JSON array: {@code []}, or values parted by commas; no comma after the last.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonArrayParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        JsonArrayNode array = new JsonArrayNode();

        JsonParsing.expect(cursor, BasicToken.T_OPEN_BRACKET, "'['");

        if (!JsonParsing.isNext(cursor, BasicToken.T_CLOSE_BRACKET)) {
            do {
                context.getParser(JsonValueParser.class).parse(cursor, array, context);
            } while (JsonParsing.consumeComma(cursor));
        }

        JsonParsing.expect(cursor, BasicToken.T_CLOSE_BRACKET, "',' or ']'");
        parent.add(array);
    }
}
