package org.jmouse.template.parsing;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.*;
import org.jmouse.template.node.BodyNode;
import org.jmouse.template.node.PrintNode;
import org.jmouse.template.node.RawTextNode;

import static org.jmouse.template.TemplateToken.*;

/**
 * 🏗️ The root parser responsible for processing the entire template.
 * This parser delegates expressions to {@link PrimaryExpressionParser}.
 *
 * <p>
 * Parses:
 * - Plain text as {@link RawTextNode}
 * - Print expressions {@code {{ expression }}} as {@link PrintNode}
 * - Execution expressions {@code {% expression %}} as {@link BodyNode}
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RootParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {

        if (cursor.isCurrent(T_RAW_TEXT)) {
            // 📝 Raw text node
            parent.add(new RawTextNode(cursor.peek().value()));
            cursor.next();
        } else if (cursor.isCurrent(T_OPEN_PRINT)) {
            // 🏗️ Handling print expression: {{ expression }}
            cursor.ensure(T_OPEN_PRINT);

            // ✅ Parse statements inside {{ statement }} block
            Node node = context.getParser(ExpressionParser.class).parse(cursor, context);

            // ✅ Expect closing braces "}}"
            cursor.ensure(T_CLOSE_PRINT);

            // ✅ Wrap expression inside PrintNode
            if (node instanceof ExpressionNode expression) {
                parent.add(new PrintNode(expression));
            }
        } else if (cursor.isCurrent(T_OPEN_EXPRESSION)) {
            // 🏗️ Handling execution expression: "{%"
            cursor.ensure(T_OPEN_EXPRESSION);
            Token currentToken = cursor.peek();

            // create body node (container)
            BodyNode body = new BodyNode();

            TagParser tagParser = context.getTagParser(currentToken.value());

            if (tagParser == null) {
                throw new ParseException("No tag parser found: '%s'".formatted(currentToken.value()));
            }

            body.add(tagParser.parse(cursor, context));

            // ✅ Expect closing expression tag: "%}"
            cursor.ensure(T_CLOSE_EXPRESSION);

            // ✅ Add parsed body to parent
            parent.add(body);
        } else {
            // 🚨 Unexpected token
            cursor.expect(BasicToken.T_EOL);
        }

    }
}
