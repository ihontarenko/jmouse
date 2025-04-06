package org.jmouse.el.core.parser.sub;

import org.jmouse.el.core.lexer.BasicToken;
import org.jmouse.el.core.lexer.TokenCursor;
import org.jmouse.el.core.node.ExpressionNode;
import org.jmouse.el.core.node.Node;
import org.jmouse.el.core.node.expression.KeyValueNode;
import org.jmouse.el.core.node.expression.MapNode;
import org.jmouse.el.core.parser.ExpressionParser;
import org.jmouse.el.core.parser.OperatorParser;
import org.jmouse.el.core.parser.Parser;
import org.jmouse.el.core.parser.ParserContext;

/**
 * Parses a comma-separated list of key-value pairs.
 * <p>
 * This parser uses an {@link OperatorParser} to parse each key and value,
 * creates a {@link KeyValueNode} for each pair (separated by a colon), and adds them
 * to a parent node (typically a {@link MapNode}).
 * </p>
 */
public class KeyValueParser implements Parser {

    /**
     * Parses key-value pairs from the token stream and adds them to the parent node.
     *
     * @param cursor  the token stream cursor
     * @param parent  the node that will contain the key-value pairs
     * @param context the parser context for retrieving sub-parsers
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Parser parser = context.getParser(ExpressionParser.class);
        do {
            KeyValueNode kv = new KeyValueNode();

            Node key = parser.parse(cursor, context);
            kv.setKey((ExpressionNode) key);

            cursor.ensure(BasicToken.T_COLON);

            Node value = parser.parse(cursor, context);
            kv.setValue((ExpressionNode) value);

            parent.add(kv);
        } while (cursor.isCurrent(BasicToken.T_COMMA) && cursor.next() != null);
    }

    /**
     * Parses key-value pairs and returns them as a {@link MapNode}.
     *
     * @param cursor  the token stream cursor
     * @param context the parser context for retrieving sub-parsers
     * @return a {@link MapNode} containing the parsed key-value pairs
     */
    @Override
    public Node parse(TokenCursor cursor, ParserContext context) {
        Node values = new MapNode();
        parse(cursor, values, context);
        return values;
    }
}
