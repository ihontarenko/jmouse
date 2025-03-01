package org.jmouse.template.parser;

import org.jmouse.template.lexer.BasicToken;
import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenCursor;
import org.jmouse.template.node.BasicNode;
import org.jmouse.template.node.Node;

/**
 * Represents a parser that processes tokens from a {@link TokenCursor} and constructs a hierarchical node structure.
 *
 * <p>Parsers read a sequence of tokens and convert them into a structured representation,
 * such as an abstract syntax tree (AST) or another hierarchical format.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Parser {

    /**
     * A default empty token used as a placeholder for node initialization.
     */
    Token EMPTY_TOKEN = new Token("token-container", BasicToken.T_UNKNOWN, -1, -1, -1);

    /**
     * Parses tokens from the given {@link TokenCursor} and constructs a node tree.
     *
     * @param cursor  the cursor providing token access
     * @param parent  the parent node where parsed nodes will be attached
     * @param context the parsing context containing necessary metadata
     */
    void parse(TokenCursor cursor, Node parent, ParserContext context);

    /**
     * Parses tokens and returns the first constructed node.
     *
     * @param cursor  the token cursor providing the token stream
     * @param context the parsing context
     * @return the root node of the parsed structure
     */
    default Node parse(TokenCursor cursor, ParserContext context) {
        Node node = BasicNode.forToken(EMPTY_TOKEN);

        parse(cursor, node, context);

        return node.first();
    }
}
