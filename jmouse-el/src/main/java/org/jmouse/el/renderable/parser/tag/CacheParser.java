package org.jmouse.el.renderable.parser.tag;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.CursorMatcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.renderable.node.CacheNode;
import org.jmouse.el.renderable.parser.TemplateParser;

import static org.jmouse.el.renderable.lexer.TemplateToken.*;

/**
 * Parses the {@code cache} tag, which captures the rendered output of its body
 * under a computed cache key.
 * <p>
 * Syntax:
 * <pre>
 *   {% cache &lt;keyExpression&gt; %} ... {% endcache %}
 * </pre>
 * The {@code keyExpression} is evaluated to determine the cache key, and the
 * inner content is parsed and stored under that key. Subsequent renders can
 * retrieve cached content by the same key.
 * </p>
 */
public class CacheParser implements TagParser {

    /**
     * Parses a {@code cache} tag from the token stream.
     *
     * @param cursor  the token cursor positioned at the start of the cache tag
     * @param context the parser context providing access to sub-parsers
     * @return a {@link CacheNode} representing the parsed cache directive
     */
    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        // Consume '{% cache %}'
        cursor.ensure(T_CACHE);

        // Prepare to parse the inner body up to '{% endcache %}'
        TemplateParser       parser  = (TemplateParser) context.getParser(TemplateParser.class);
        CacheNode            cache   = new CacheNode();
        Matcher<TokenCursor> matcher = CursorMatcher.sequence(T_OPEN_EXPRESSION, T_END_CACHE);

        // Parse the key expression
        ExpressionNode key = (ExpressionNode) context.getParser(ExpressionParser.class).parse(cursor, context);
        cache.setKey(key);

        // Consume closing '%}'
        cursor.ensure(T_CLOSE_EXPRESSION);

        // Parse the body content until the endcache marker
        Node body = parser.parse(cursor, context, matcher);
        cache.setContent(body);

        // Consume '{% endcache %}'
        cursor.ensure(T_END_CACHE);

        return cache;
    }

    /**
     * Returns the name of the tag that this parser handles.
     *
     * @return the string {@code "cache"}
     */
    @Override
    public String getName() {
        return "cache";
    }
}
