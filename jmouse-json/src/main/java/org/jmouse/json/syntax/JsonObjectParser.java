package org.jmouse.json.syntax;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.json.JsonTexts;
import org.jmouse.json.node.JsonMemberNode;
import org.jmouse.json.node.JsonObjectNode;

/**
 * 🧾 A JSON object: {@code {}}, or members — a name in double quotes, a colon, a value — parted by commas; no comma after the
 * last.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonObjectParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        JsonObjectNode object = new JsonObjectNode();

        JsonParsing.expect(cursor, BasicToken.T_OPEN_CURLY, "'{'");

        if (!JsonParsing.isNext(cursor, BasicToken.T_CLOSE_CURLY)) {
            do {
                Token          name   = JsonParsing.expect(cursor, BasicToken.T_STRING, "a member's name in double quotes");
                JsonMemberNode member = new JsonMemberNode(JsonTexts.decode(name.value()));

                JsonParsing.expect(cursor, BasicToken.T_COLON, "':' after a member's name");
                context.getParser(JsonValueParser.class).parse(cursor, member, context);
                object.add(member);
            } while (JsonParsing.consumeComma(cursor));
        }

        JsonParsing.expect(cursor, BasicToken.T_CLOSE_CURLY, "',' or '}'");
        parent.add(object);
    }
}
