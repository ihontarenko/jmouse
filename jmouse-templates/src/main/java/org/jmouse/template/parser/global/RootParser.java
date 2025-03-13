package org.jmouse.template.parser.global;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.*;
import org.jmouse.template.parser.ExpressionParser;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.ParserContext;

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
                cursor.expect(T_OPEN_PRINT);

                // ✅ Parse statements inside {{ statement }} block
                Node node = parser.parse(cursor, context);

                // ✅ Expect closing braces "}}"
                cursor.expect(T_CLOSE_PRINT);

                // ✅ Wrap expression inside PrintNode
                if (node instanceof ExpressionNode expression) {
                    parent.add(new PrintNode(expression));
                }
            } else if (cursor.isCurrent(T_OPEN_EXPRESSION)) {
                // 🏗️ Handling execution expression: "{%"
                cursor.next();

                // create body node (container)
                BodyNode body = new BodyNode();

                // ✅ Parse statements inside {% statement %} block
                parser.parse(cursor, body, context);

                // ✅ Expect closing expression tag: "%}"
                cursor.expect(T_CLOSE_EXPRESSION);

                // ✅ Add parsed body to parent
                parent.add(body);
            } else {
                // 🚨 Unexpected token
                cursor.expect(T_EOL);
            }
        }
    }
}
