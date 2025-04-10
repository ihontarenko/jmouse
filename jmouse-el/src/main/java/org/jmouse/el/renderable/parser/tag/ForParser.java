package org.jmouse.el.renderable.parser.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.PropertyParser;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.ForNode;
import org.jmouse.el.renderable.parser.TemplateParser;

import static org.jmouse.el.lexer.BasicToken.T_IDENTIFIER;
import static org.jmouse.el.renderable.lexer.TemplateToken.*;

/**
 * Parses a "for" loop tag.
 * <p>
 * This parser processes tags with the following structure:
 * <pre>
 *   {% for item in iterable %} ... {% else %} ... {% endfor %}
 * </pre>
 * It extracts the loop item, the iterable expression, and the body (with an optional else block).
 * </p>
 */
public class ForParser implements TagParser {

    /**
     * Creates a stopper matcher that terminates the loop body parsing.
     * <p>
     * The stopper matches either the beginning of an "else" block or the end of the for-loop.
     * </p>
     *
     * @return a {@link Matcher} for stopping at the correct token
     */
    public static Matcher<TokenCursor> stopper() {
        Matcher<TokenCursor> matcher = Matcher.constant(false);

        matcher = matcher.or(CursorMatcher.sequence(T_OPEN_EXPRESSION, T_ELSE));
        matcher = matcher.or(CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_FOR));

        return matcher;
    }

    /**
     * Parses a "for" tag from the provided token cursor and parser context.
     * <p>
     * This method expects a tag starting with {% for %}, then the loop variable,
     * followed by "in" and an expression for the iterable. It then parses the loop body,
     * optionally an else block, and finally expects the {% endfor %} token.
     * </p>
     *
     * @param cursor  the token cursor at the beginning of the for tag
     * @param context the parser context used to access sub-parsers
     * @return a {@link Node} representing the parsed for loop
     */
    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        TemplateParser       parser  = (TemplateParser) context.getParser(TemplateParser.class);
        ForNode              node    = new ForNode();
        Matcher<TokenCursor> matcher = stopper();

        // Ensure the for tag starts correctly.
        cursor.ensure(TemplateToken.T_FOR);

        // Expect and register the loop variable identifier.
        node.setItem(cursor.ensure(T_IDENTIFIER).value());

        // Expect the 'in' token.
        cursor.ensure(T_IN);

        // Parse the iterable expression.
        node.setIterable((ExpressionNode) context.getParser(PropertyParser.class).parse(cursor, context));

        // Expect closing tag for the opening for statement.
        cursor.ensure(T_CLOSE_EXPRESSION);

        // Parse the body of the loop until a stopper token is encountered.
        Node body = parser.parse(cursor, context, matcher);
        node.setBody(body);

        // Optionally parse the else clause if present.
        if (cursor.currentIf(T_ELSE)) {
            cursor.ensure(T_CLOSE_EXPRESSION);
            node.setEmpty(parser.parse(cursor, context, matcher));
        }

        // Ensure the for tag ends with the correct end token.
        cursor.ensure(T_END_FOR);

        return node;
    }

    /**
     * Returns the name of the tag this parser handles.
     *
     * @return the string "for"
     */
    @Override
    public String getName() {
        return "for";
    }
}
