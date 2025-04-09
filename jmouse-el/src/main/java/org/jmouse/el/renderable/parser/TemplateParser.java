package org.jmouse.el.renderable.parser;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.renderable.node.ContainerNode;

/**
 * 🏗️ The template parser that processes the entire template file.
 * <p>
 * This parser iterates over the token stream, invoking the {@link RootParser} for each segment
 * (raw text, print expressions, and execution expressions), and aggregates them into a single
 * {@link ContainerNode} representing the complete template.
 * </p>
 * <p>
 * An overloaded parse method allows stopping when a specified matcher condition is met.
 * </p>
 *
 * @author ...
 */
public class TemplateParser implements Parser {

    /**
     * Parses the entire template from the token stream and adds the resulting nodes to the parent.
     *
     * @param cursor  the token cursor
     * @param parent  the parent node that will contain the parsed template nodes
     * @param context the parser context for retrieving sub-parsers
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        ContainerNode container = new ContainerNode();

        while (cursor.hasNext()) {
            // Delegate parser to the RootParser for each section.
            container.add(context.getParser(RootParser.class).parse(cursor, context));
        }

        parent.add(container);
    }

    /**
     * Parses template content until the provided matcher condition is met.
     *
     * @param cursor  the token cursor
     * @param context the parser context for retrieving sub-parsers
     * @param matcher a matcher that defines the stopping condition
     */
    public Node parse(TokenCursor cursor, ParserContext context, Matcher<TokenCursor> matcher) {
        ContainerNode container = new ContainerNode();
        Parser        parser    = context.getParser(RootParser.class);

        while (cursor.hasNext() && !matcher.matches(cursor)) {
            container.add(parser.parse(cursor, context));
        }

        if (matcher.matches(cursor)) {
            cursor.next();
        }

        return container;
    }
}
