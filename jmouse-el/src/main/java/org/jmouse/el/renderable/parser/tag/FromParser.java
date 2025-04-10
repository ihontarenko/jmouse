package org.jmouse.el.renderable.parser.tag;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.NameSetNode;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.parser.sub.NamesParser;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.node.FromNode;

/**
 * A tag parser for handling the "from" tag.
 * <p>
 * This parser processes tags in the form of:
 * <pre>
 *     {% from &lt;source&gt; import &lt;names&gt; %}
 * </pre>
 * where the source is an expression that determines the template to import from,
 * and the names are a list of identifiers specifying which definitions to import.
 * </p>
 */
public class FromParser implements TagParser {

    /**
     * Parses a "from" tag from the given token cursor.
     * <p>
     * The parser ensures that the current token corresponds to the T_FROM token,
     * parses an expression for the source using a {@link LiteralParser}, ensures an
     * T_IMPORT token follows, and then parses a list of names using a {@link NamesParser}.
     * A new {@link FromNode} is created with the parsed source and names.
     * </p>
     *
     * @param cursor  the token cursor pointing at the current position in the input stream
     * @param context the parser context providing access to sub-parsers and conversion utilities
     * @return a {@link Node} representing the parsed "from" tag
     */
    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        // Ensure the tag starts with "from"
        cursor.ensure(TemplateToken.T_FROM);

        // Parse the source expression (what template to import from)
        ExpressionNode source = (ExpressionNode) context.getParser(LiteralParser.class)
                .parse(cursor, context);

        // Ensure the next token is "import"
        cursor.ensure(TemplateToken.T_IMPORT);

        // Parse the list of names to import
        NameSetNode names = (NameSetNode) context.getParser(NamesParser.class)
                .parse(cursor, context);

        // Create and populate the FromNode with the parsed source and names
        FromNode node = new FromNode();

        node.setPath(source);
        node.setNames(names);

        return node;
    }

    /**
     * Returns the name of the tag that this parser handles.
     *
     * @return a string representing the tag name ("from")
     */
    @Override
    public String getName() {
        return "from";
    }
}
