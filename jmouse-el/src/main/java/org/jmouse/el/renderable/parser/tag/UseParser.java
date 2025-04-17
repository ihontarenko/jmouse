package org.jmouse.el.renderable.parser.tag;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.LiteralNode;
import org.jmouse.el.node.expression.NameSetNode;
import org.jmouse.el.node.expression.literal.StringLiteralNode;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.parser.sub.NamesParser;
import org.jmouse.el.renderable.node.UseNode;

import static org.jmouse.el.renderable.lexer.TemplateToken.*;

/**
 * Parses the {@code use} tag, which allows importing macros or blocks
 * from another template into the current template's registry.
 * <p>
 * Syntax examples:
 * <ul>
 *   <li>{@code {% use <expr> get macro %}} — import all macros.</li>
 *   <li>{@code {% use <expr> get block as alias %}} — import a block under an alias.</li>
 *   <li>{@code {% use <expr> get macro import name1 as default_name, name2 %}} — import specific macros.</li>
 * </ul>
 * </p>
 */
public class UseParser implements TagParser {

    /**
     * Parses a {@code use} tag from the token stream.
     *
     * @param cursor  the token cursor positioned at the start of the tag
     * @param context the parser context providing sub-parsers
     * @return a {@link UseNode} representing the parsed import directive
     */
    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        // Consume '{% use %}'
        cursor.ensure(T_USE);

        // Parse the path expression to the other template
        ExpressionNode path = (ExpressionNode) context.getParser(ExpressionParser.class).parse(cursor, context);

        // Consume 'get' keyword
        cursor.ensure(T_GET);

        // Determine whether we import macros or blocks
        Token   token = cursor.ensure(T_MACRO, T_BLOCK);
        UseNode use   = new UseNode();

        use.setPath(path);
        use.setType(token.type());

        // Optional 'as' alias clause
        if (cursor.currentIf(T_AS)) {
            LiteralNode<String> alias = new StringLiteralNode(cursor.ensure(BasicToken.T_IDENTIFIER).value());
            use.setAlias(alias);
        }
        // Optional 'import' names clause
        else if (cursor.currentIf(T_IMPORT)) {
            NameSetNode names = (NameSetNode) context.getParser(NamesParser.class).parse(cursor, context);
            use.setNames(names);
        }

        return use;
    }

    /**
     * Returns the name of the tag handled by this parser.
     *
     * @return the string "use"
     */
    @Override
    public String getName() {
        return "use";
    }
}
