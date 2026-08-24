package org.jmouse.query.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.query.el.QueryParseException;
import org.jmouse.query.el.lexer.QueryToken;
import org.jmouse.query.el.node.ClauseNode;
import org.jmouse.query.el.node.ColumnsNode;
import org.jmouse.query.el.node.GroupNode;
import org.jmouse.query.el.node.HavingNode;
import org.jmouse.query.el.node.OrderNode;
import org.jmouse.query.el.node.WhereNode;

/**
 * Reads the clauses a {@code view} or {@code function} block is made of.
 *
 * <p>Not a registered {@link org.jmouse.el.parser.Parser}: a clause is only ever read from inside a
 * block, so offering it to dispatch would let {@code where} start a top-level declaration. It is held
 * as one class rather than three because the three share the thing that is actually easy to get wrong —
 * ⚠️ <strong>knowing where a clause ends.</strong></p>
 *
 * <h2>⚠️ Where a clause ends, and why it is not obvious</h2>
 *
 * <p>A clause has no terminator. {@code where} runs until the next clause keyword or the closing brace,
 * and the expression parser stops on its own when it meets a token it cannot continue with. That works
 * because every clause keyword is a keyword — which is exactly why the reserved words are lexed rather
 * than left as identifiers: were {@code group} an identifier, {@code where x > 1 group y} would parse
 * the {@code group y} into the condition as an implicit multiplication and answer something.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final class ClauseParser {

    private ClauseParser() {
    }

    /**
     * Whether the cursor is on a word that opens a clause.
     *
     * @param cursor the cursor to inspect
     * @return {@code true} when a clause follows
     */
    static boolean opensClause(TokenCursor cursor) {
        return cursor.isCurrent(QueryToken.T_WHERE, QueryToken.T_ORDER, QueryToken.T_COLUMNS,
                QueryToken.T_GROUP, QueryToken.T_HAVING);
    }

    /**
     * Reads one clause.
     *
     * @param cursor  the cursor, positioned on the clause keyword
     * @param context the parser context
     * @return the clause
     */
    static ClauseNode parse(TokenCursor cursor, ParserContext context) {
        Token opening = cursor.peek();

        if (cursor.consumeIf(QueryToken.T_WHERE)) {
            return parseWhere(cursor, context);
        }

        if (cursor.consumeIf(QueryToken.T_ORDER)) {
            return parseOrder(cursor, context);
        }

        if (cursor.consumeIf(QueryToken.T_COLUMNS)) {
            return parseColumns(cursor, context);
        }

        if (cursor.consumeIf(QueryToken.T_GROUP)) {
            return parseGroup(cursor, context);
        }

        if (cursor.consumeIf(QueryToken.T_HAVING)) {
            return parseHaving(cursor, context);
        }

        throw QueryParseException.notAClause(
                opening, "'where', 'order', 'columns', 'group' and 'having'");
    }

    private static ClauseNode parseGroup(TokenCursor cursor, ParserContext context) {
        GroupNode group = new GroupNode();

        do {
            group.addKey(expression(cursor, context));
        } while (cursor.consumeIf(BasicToken.T_COMMA));

        return group;
    }

    private static ClauseNode parseHaving(TokenCursor cursor, ParserContext context) {
        HavingNode having = new HavingNode();

        having.setCondition(expression(cursor, context));

        return having;
    }

    private static ClauseNode parseWhere(TokenCursor cursor, ParserContext context) {
        WhereNode where = new WhereNode();

        where.setCondition(expression(cursor, context));

        return where;
    }

    private static ClauseNode parseOrder(TokenCursor cursor, ParserContext context) {
        return parseKeys(cursor, context);
    }

    /**
     * The sort keys themselves, without the {@code order} keyword.
     *
     * <p>⚠️ Reachable from {@link OrderParser} as well as from here, because a sort arrives on its own in
     * a URL and a document is not the only place one is written. Two readers of the same clause would be
     * two grammars for it, and they would drift the first time a direction word was added.</p>
     *
     * @param cursor  positioned at the first key
     * @param context the parser context
     * @return the clause
     */
    static OrderNode parseKeys(TokenCursor cursor, ParserContext context) {
        OrderNode order = new OrderNode();

        do {
            Expression key = expression(cursor, context);

            order.addKey(key, direction(cursor));
        } while (cursor.consumeIf(BasicToken.T_COMMA));

        return order;
    }

    private static ClauseNode parseColumns(TokenCursor cursor, ParserContext context) {
        ColumnsNode columns = new ColumnsNode();

        do {
            Expression projection = expression(cursor, context);
            String     alias      = null;

            if (cursor.consumeIf(QueryToken.T_AS)) {
                alias = cursor.ensure(BasicToken.T_IDENTIFIER, BasicToken.T_STRING).value();
            }

            columns.addProjection(projection, alias);
        } while (cursor.consumeIf(BasicToken.T_COMMA));

        return columns;
    }

    /**
     * Reads {@code asc} / {@code desc}, or nothing.
     *
     * <p>⚠️ Returns {@code null} rather than defaulting to ascending. The default belongs to whoever
     * runs the query; the node's job is to record what was <em>written</em>, so that a document written
     * back out says what its author said.</p>
     */
    private static OrderNode.Direction direction(TokenCursor cursor) {
        if (cursor.consumeIf(QueryToken.T_ASC)) {
            return OrderNode.Direction.ASCENDING;
        }

        if (cursor.consumeIf(QueryToken.T_DESC)) {
            return OrderNode.Direction.DESCENDING;
        }

        return null;
    }

    private static Expression expression(TokenCursor cursor, ParserContext context) {
        return (Expression) context.getParser(ExpressionParser.class).parse(cursor, context);
    }
}
