package org.jmouse.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Node;

/**
 * General interface for all token-based parsers in the expression language system.
 * <p>
 * A {@code Parser} is responsible for consuming a {@link TokenCursor} and
 * constructing a hierarchy of {@link Node} instances that represent a portion of the
 * abstract syntax tree (AST).
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Parser {

    /**
     * Parses a stream of tokens starting at the given cursor and appends
     * resulting nodes into the specified parent node.
     *
     * @param cursor  the token cursor to read from
     * @param parent  the parent node to attach parsed children to
     * @param context the parser context for stateful or scoped operations
     */
    void parse(TokenCursor cursor, Node parent, ParserContext context);

    /**
     * Parses a stream of tokens and returns the first parsed node.
     * <p>
     * Internally, a temporary {@link BasicNode} is used as a container for collecting results.
     * Only the first child is returned.
     * </p>
     *
     * @param cursor  the token cursor to read from
     * @param context the parser context
     * @return the first parsed node
     */
    default Node parse(TokenCursor cursor, ParserContext context) {
        Node container = BasicNode.forToken(cursor.current());
        parse(cursor, container, context);
        return container.getFirst();
    }

    default boolean supports(TokenCursor cursor) {
        return false;
    }

}
