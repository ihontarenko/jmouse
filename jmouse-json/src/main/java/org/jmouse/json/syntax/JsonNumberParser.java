package org.jmouse.json.syntax;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.json.JsonSyntaxException;
import org.jmouse.json.node.JsonScalarNode;

import java.util.regex.Pattern;

/**
 * 🧾 A JSON number: an optional minus right before it, then a whole part with no leading zero, an optional fraction and
 * exponent — its value read by the engine's own rule ({@link LiteralParser#numericLiteral}), so JSON's {@code 5} is jME's
 * {@code 5}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonNumberParser implements Parser {

    /** What the engine's number token may be in JSON: no leading zero. Its sign is a token of its own. */
    private static final Pattern UNSIGNED = Pattern.compile("(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");

    private static final String A_NUMBER = "a number — digits with no leading zero, a fraction, an exponent";

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        JsonParsing.skipLines(cursor);

        Token   sign     = cursor.peek();
        boolean negative = cursor.isCurrent(BasicToken.T_MINUS);

        if (negative) {
            cursor.next();
        }

        Token digits = cursor.peek();

        // The sign stands right before its digits: "- 1" is no JSON number.
        boolean signApart = negative && digits.offset() != sign.offset() + 1;

        if (!cursor.isCurrent(BasicToken.T_NUMERIC) || !UNSIGNED.matcher(digits.value()).matches() || signApart) {
            throw new JsonSyntaxException(A_NUMBER, digits);
        }

        cursor.next();

        parent.add(new JsonScalarNode(LiteralParser.numericLiteral(negative ? "-" + digits.value() : digits.value()).getValue()));
    }
}
