package org.jmouse.el.renderable.parsing;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.*;
import org.jmouse.el.renderable.EmptyNode;
import org.jmouse.el.renderable.node.ContainerNode;
import org.jmouse.el.renderable.node.PrintNode;
import org.jmouse.el.renderable.node.RawTextNode;

import static org.jmouse.el.renderable.lexer.TemplateToken.*;

/**
 * 🏗️ The root parser responsible for processing the entire template.
 * <p>
 * Parses:
 * <ul>
 *   <li>Plain text as {@link RawTextNode}</li>
 *   <li>Print expressions (e.g. <code>{{ expression }}</code>) as {@link PrintNode}</li>
 *   <li>Execution expressions (e.g. <code>{% expression %}</code>) as {@link ContainerNode}</li>
 * </ul>
 * </p>
 * <p>
 * Delegates expression parsing to {@link ExpressionParser} and tag parsing to appropriate {@link TagParser} implementations.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RootParser implements Parser {

    /**
     * Parses the template content from the token stream and attaches the resulting nodes to the parent.
     *
     * @param cursor  the token cursor
     * @param parent  the parent node to which parsed nodes are added
     * @param context the parser context for retrieving sub-parsers
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {

        if (cursor.isCurrent(T_RAW_TEXT)) {
            // Raw text node
            parent.add(new RawTextNode(cursor.peek().value()));
            cursor.next();
        } else if (cursor.isCurrent(T_OPEN_PRINT)) {
            // Print expression: {{ expression }}
            cursor.ensure(T_OPEN_PRINT);

            if (context.getParser(ExpressionParser.class).parse(cursor, context) instanceof ExpressionNode expression) {
                parent.add(new PrintNode(expression));
            }

            cursor.ensure(T_CLOSE_PRINT);

        } else if (cursor.isCurrent(T_OPEN_EXPRESSION)) {
            // Execution expression: {% expression %}
            cursor.ensure(T_OPEN_EXPRESSION);

            Token token = cursor.peek();

            // Retrieve tag parser based on the token value.
            TagParser tagParser = context.getTagParser(token.value());

            if (tagParser == null) {
                throw new ParseException("No tag parser found: '%s'".formatted(token.value()));
            }

            Node tagNode = tagParser.parse(cursor, context);

            cursor.ensure(T_CLOSE_EXPRESSION);
            parent.add(tagNode);
        } else {
            // Unexpected token; expect end-of-line.
            cursor.expect(BasicToken.T_EOL);
            parent.add(new EmptyNode());
        }
    }
}
