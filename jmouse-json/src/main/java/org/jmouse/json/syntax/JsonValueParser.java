package org.jmouse.json.syntax;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.json.JsonSyntaxException;
import org.jmouse.json.node.JsonScalarNode;

/**
 * 🧾 One JSON value: an object, an array, a string, a number, or one of the words {@code true}, {@code false}, {@code null} —
 * and only those words: the engine reads {@code none} as null too, JSON does not.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonValueParser implements Parser {

    private static final String A_VALUE = "a value — an object, an array, a string, a number, true, false or null";

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        JsonParsing.skipLines(cursor);

        Token token = cursor.peek();

        if (!(token.type() instanceof BasicToken type)) {
            throw new JsonSyntaxException(A_VALUE, token);
        }

        switch (type) {
            case T_OPEN_CURLY -> context.getParser(JsonObjectParser.class).parse(cursor, parent, context);
            case T_OPEN_BRACKET -> context.getParser(JsonArrayParser.class).parse(cursor, parent, context);
            case T_STRING -> context.getParser(JsonTextParser.class).parse(cursor, parent, context);
            case T_NUMERIC, T_MINUS -> context.getParser(JsonNumberParser.class).parse(cursor, parent, context);
            case T_TRUE, T_FALSE -> {
                cursor.next();
                parent.add(new JsonScalarNode(type == BasicToken.T_TRUE));
            }
            case T_NULL -> {
                if (!"null".equals(token.value())) {
                    throw new JsonSyntaxException(A_VALUE, token);
                }
                cursor.next();
                parent.add(new JsonScalarNode(null));
            }
            default -> throw new JsonSyntaxException(A_VALUE, token);
        }
    }
}
