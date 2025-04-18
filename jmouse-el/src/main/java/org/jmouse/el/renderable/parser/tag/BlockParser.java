package org.jmouse.el.renderable.parser.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.node.BlockNode;
import org.jmouse.el.renderable.parser.TemplateParser;

import static org.jmouse.el.renderable.lexer.TemplateToken.*;

/**
 * Parses the {@code block} tag, which defines a named, overridable section of a template.
 * <p>
 * Syntax:
 * <pre>
 *   {% block &lt;name&gt; %} ... {% endblock %}
 * </pre>
 * The parser reads the block name as a literal expression, then captures all content
 * until the matching {@code endblock} tag.
 * </p>
 */
public class BlockParser implements TagParser {

    /**
     * Parses a block tag from the token stream.
     *
     * @param cursor  the token cursor positioned at the start of the block tag
     * @param context the parser context providing access to sub-parsers
     * @return a {@link BlockNode} representing the parsed block
     */
    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        TemplateParser parser = (TemplateParser) context.getParser(TemplateParser.class);
        BlockNode      block  = new BlockNode();
        // Stop parsing when {% endblock %} is encountered
        Matcher<TokenCursor> matcher = CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_BLOCK);

        // Consume '{% block %}'
        cursor.ensure(T_BLOCK);
        // Parse the block name (literal expression)
        ExpressionNode name = (ExpressionNode)
                context.getParser(LiteralParser.class).parse(cursor, context);

        // Consume 'override' keyword if any
        block.setOverride(cursor.currentIf(T_OVERRIDE));

        // Consume closing '%}'
        cursor.ensure(T_CLOSE_EXPRESSION);
        // Parse body until endblock
        Node body = parser.parse(cursor, context, matcher);

        block.setName(name);
        block.setBody(body);

        // Consume '{% endblock %}'
        cursor.ensure(T_END_BLOCK);

        return block;
    }

    /**
     * Returns the name of the tag that this parser handles.
     *
     * @return the string {@code "block"}
     */
    @Override
    public String getName() {
        return "block";
    }
}
