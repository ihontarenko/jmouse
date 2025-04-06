package org.jmouse.el.core.parser;

import org.jmouse.el.core.lexer.BasicToken;
import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.Node;
import org.jmouse.el.core.node.expression.MapNode;
import org.jmouse.el.core.parser.sub.KeyValueParser;

/**
 * Parses a map literal expression.
 * <p>
 * This parser expects the map literal to be enclosed in curly braces.
 * If the map is non-empty, it uses the {@link KeyValueParser} to parse key-value pairs
 * and aggregates them into a {@link MapNode}, which is then added to the parent node.
 * </p>
 */
public class MapParser implements Parser {

    /**
     * Parses a map literal from the token stream.
     * <p>
     * The parser ensures the map literal starts with '{' and ends with '}'. If the map is non-empty,
     * key-value pairs are parsed using the {@link KeyValueParser}.
     * </p>
     *
     * @param cursor  the token cursor providing the token stream
     * @param parent  the parent node to which the parsed map node is added
     * @param context the parser context used to retrieve sub-parsers
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Node map = new MapNode();

        cursor.ensure(BasicToken.T_OPEN_CURLY);
        if (!cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            context.getParser(KeyValueParser.class).parse(cursor, map, context);
        }
        cursor.ensure(BasicToken.T_CLOSE_CURLY);

        parent.add(map);
    }
}
