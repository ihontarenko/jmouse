package org.jmouse.json.syntax;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.json.JsonTexts;
import org.jmouse.json.node.JsonScalarNode;

/**
 * 🧾 A JSON string: double quotes, its escapes read as {@link JsonTexts} reads them; a single-quoted string is jME's, not JSON's.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonTextParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        String written = JsonParsing.expect(cursor, BasicToken.T_STRING, "a string").value();
        parent.add(new JsonScalarNode(JsonTexts.decode(written)));
    }
}
