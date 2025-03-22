package org.jmouse.template.parsing;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.*;
import org.jmouse.template.node.BodyNode;
import org.jmouse.template.node.PrintNode;
import org.jmouse.template.node.RawTextNode;

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
public class TemplateParser implements Parser {

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        BodyNode body = new BodyNode();

        while (cursor.hasNext()) {
            body.add(context.getParser(RootParser.class).parse(cursor, context));
        }

        parent.add(body);
    }
}
