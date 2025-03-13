package org.jmouse.template.parser.core;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.*;
import org.jmouse.template.node.renderable.BodyNode;
import org.jmouse.template.node.renderable.PrintNode;
import org.jmouse.template.node.renderable.RawTextNode;
import org.jmouse.template.parser.*;

import static org.jmouse.template.lexer.BasicToken.*;
import static org.jmouse.template.lexer.TemplateToken.*;

/**
 * 🏗️ The root parser responsible for processing the entire template.
 * This parser delegates expressions to {@link ExpressionParser}.
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
        Parser parser = context.getParser(OperatorParser.class);

        while (cursor.hasNext()) {

            Token token = cursor.peek();

            if (cursor.isCurrent(T_RAW_TEXT)) {
                // 📝 Raw text node
                parent.add(new RawTextNode(token.value()));
                cursor.next();
            } else if (cursor.isCurrent(T_OPEN_PRINT)) {
                // 🏗️ Handling print expression: {{ expression }}
                cursor.ensure(T_OPEN_PRINT);

                // consume open tag
                cursor.next();

                // ✅ Parse statements inside {{ statement }} block
                Node node = parser.parse(cursor, context);

                // ✅ Expect closing braces "}}"
                cursor.ensure(T_CLOSE_PRINT);

                // ✅ Wrap expression inside PrintNode
                if (node instanceof ExpressionNode expression) {
                    parent.add(new PrintNode(expression));
                }
            } else if (cursor.isCurrent(T_OPEN_EXPRESSION)) {
                // 🏗️ Handling execution expression: "{%"
                cursor.next();
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
                cursor.expect(T_EOL);
            }
        }
    }
}
