package org.jmouse.el.renderable.parser.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.MapNode;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.MapParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.EmbedNode;
import org.jmouse.el.renderable.parser.TemplateParser;

import static org.jmouse.el.renderable.lexer.TemplateToken.*;

/**
 * Parses the {@code embed} tag from a view.
 * <p>
 * The embed tag is expected to have the following syntax:
 * <pre>
 *   {% embed <expression> [with <mapExpression>] %} ... {% endembed %}
 * </pre>
 * The tag starts with the {@code T_EMBED} token, followed by an expression
 * (evaluated to a view path) and optionally a {@code with} clause for parameters.
 * Then, the tag content is parsed until the end token {@code T_END_EMBED} is encountered.
 * </p>
 */
public class EmbedParser implements TagParser {

    /**
     * Parses an embed tag, producing an {@link EmbedNode} that encapsulates both
     * the source path and the optional parameters as well as the tag body.
     *
     * @param cursor  the token cursor positioned at the start of the embed tag
     * @param context the parser context providing access to sub-parsers
     * @return an {@link Node} representing the parsed embed tag
     */
    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        // Retrieve the TemplateParser from the context.
        // Create a matcher for the tag body content between the opening and closing embed tokens.
        TemplateParser       parser  = (TemplateParser) context.getParser(TemplateParser.class);
        EmbedNode            embed   = new EmbedNode();
        Matcher<TokenCursor> matcher = CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_EMBED);

        // Ensure the tag starts with the EMBED token.
        cursor.ensure(TemplateToken.T_EMBED);

        // Parse the source expression for the embed path.
        ExpressionNode path = (ExpressionNode) context.getParser(LiteralParser.class).parse(cursor, context);

        // Optionally parse the 'with' clause containing additional variables.
        if (cursor.currentIf(T_WITH)) {
            embed.setWith((MapNode) context.getParser(MapParser.class).parse(cursor, context));
        }

        // Ensure the closing expression token is encountered.
        cursor.ensure(T_CLOSE_EXPRESSION);
        // Parse the content body using the TemplateParser and the provided matcher.
        Node body = parser.parse(cursor, context, matcher);

        // Set the parsed path and body into the embed node.
        embed.setPath(path);
        embed.setBody(body);

        // Ensure the end of the embed tag is reached.
        cursor.ensure(T_END_EMBED);

        return embed;
    }

    /**
     * Returns the name of the tag handled by this parser.
     *
     * @return a string "embed" indicating the tag name
     */
    @Override
    public String getName() {
        return "embed";
    }
}
