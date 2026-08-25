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
import org.jmouse.query.el.node.LimitNode;
import org.jmouse.query.el.node.GroupNode;
import org.jmouse.query.el.node.HavingNode;
import org.jmouse.query.el.node.JoinClauseNode;
import org.jmouse.query.el.node.OrderNode;
import org.jmouse.query.el.node.WhereNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    /**
     * Whether a clause starts here.
     *
     * <p>⚠️ Permissive on purpose: a registered clause is an ordinary identifier to the lexer, so this
     * cannot tell one from a word nobody registered. Deciding that is {@link #parse}'s job, and it does it
     * with a refusal that lists what would have worked — which is a better answer than a block quietly
     * ending one line early.</p>
     */
    static boolean opensClause(TokenCursor cursor) {
        return cursor.isCurrent(QueryToken.T_WHERE, QueryToken.T_ORDER, QueryToken.T_FETCH,
                QueryToken.T_COLUMNS, QueryToken.T_GROUP, QueryToken.T_HAVING, QueryToken.T_LIMIT)
               || ClauseParsers.knows(cursor.peek().value());
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

        // ⚠️ `fetch` is the word; `columns` is what it used to be called. The reader answers to both so
        // that a stored view keeps working, and the writer emits only `fetch`.
        if (cursor.consumeIf(QueryToken.T_FETCH) || cursor.consumeIf(QueryToken.T_COLUMNS)) {
            return parseColumns(cursor, context);
        }

        if (cursor.consumeIf(QueryToken.T_GROUP)) {
            return parseGroup(cursor, context);
        }

        if (cursor.consumeIf(QueryToken.T_HAVING)) {
            return parseHaving(cursor, context);
        }

        if (cursor.consumeIf(QueryToken.T_LIMIT)) {
            return parseLimit(cursor);
        }

        if (cursor.consumeIf(QueryToken.T_JOIN)) {
            return parseJoin(cursor);
        }

        // ⚠️ Anything else is looked up by the WORD it is written with, so a clause a product registered
        // needs no token in this library. The five above keep theirs only because those tokens are
        // load-bearing elsewhere — `from`, `key` and `on` all appear in a mapping.
        return registered(cursor, context, opening);
    }

    /**
     * A clause nobody built in — {@code limit}, {@code elastic.score}, whatever was registered.
     *
     * <p>⚠️ Refused with the registered words listed, never skipped and never read as an expression. A
     * line nobody understood, silently ignored, is a query that returns the wrong rows and says
     * nothing.</p>
     */
    private static ClauseNode registered(TokenCursor cursor, ParserContext context, Token opening) {
        String keyword = keyword(cursor);

        ClauseParsers.Reader reader = ClauseParsers.reader(keyword).orElseThrow(
                () -> QueryParseException.notAClause(opening, listed()));

        Declarations.optionalColon(cursor);

        return reader.read(cursor, context);
    }

    /**
     * The word a clause is written with, {@code elastic.score} included.
     *
     * <p>⚠️ A dot at clause position can only be a namespace: a clause is the first thing on its line and
     * an attribute path is never one. So the two are read apart by WHERE they are, not by what they look
     * like — which is the same reason a keyword may be an attribute name elsewhere.</p>
     */
    private static String keyword(TokenCursor cursor) {
        StringBuilder written = new StringBuilder(Declarations.word(cursor));

        while (cursor.isCurrent(BasicToken.T_DOT)) {
            cursor.ensure(BasicToken.T_DOT);
            written.append('.').append(Declarations.word(cursor));
        }

        return written.toString();
    }

    private static String listed() {
        List<String> words = new ArrayList<>(
                List.of("where", "order", "fetch", "group", "having", "limit"));

        ClauseParsers.keywords().stream().sorted().filter(word -> !words.contains(word)).forEach(words::add);

        return words.stream().map("'%s'"::formatted).collect(Collectors.joining(", "));
    }

    /**
     * {@code join: person on person.key == request.assignee}
     *
     * <p>⚠️ Read as two attribute paths and an equality rather than as an expression. A join whose
     * condition could be anything is a join a backend cannot promise to honour — a row pipeline hashes two
     * sides on equal keys and cannot evaluate {@code a > b} without comparing every pair.</p>
     */
    private static ClauseNode parseJoin(TokenCursor cursor) {
        Declarations.optionalColon(cursor);

        JoinClauseNode join = new JoinClauseNode();

        join.setStructure(Declarations.name(cursor));

        cursor.ensure(QueryToken.T_ON);
        join.setLeft(Declarations.path(cursor));

        cursor.ensure(BasicToken.T_EQ);
        join.setRight(Declarations.path(cursor));

        return join;
    }

    /**
     * {@code limit: 50} — a count, read here rather than as an expression.
     *
     * <p>⚠️ Refused when it is not a positive whole number, by name. A limit of zero is a query that
     * cannot return anything and is never what somebody meant to write.</p>
     */
    private static ClauseNode parseLimit(TokenCursor cursor) {
        Declarations.optionalColon(cursor);

        Token written = cursor.ensure(BasicToken.T_INT, BasicToken.T_NUMERIC);
        int   count   = (int) Double.parseDouble(written.value());

        if (count <= 0) {
            throw new QueryParseException(
                    "'limit' at line %d is %d; write how many rows to bring back"
                            .formatted(written.lineNumber(), count));
        }

        LimitNode limit = new LimitNode();

        limit.setCount(count);

        return limit;
    }

    private static ClauseNode parseGroup(TokenCursor cursor, ParserContext context) {
        Declarations.optionalColon(cursor);

        GroupNode group = new GroupNode();

        do {
            group.addKey(expression(cursor, context));
        } while (cursor.consumeIf(BasicToken.T_COMMA));

        return group;
    }

    private static ClauseNode parseHaving(TokenCursor cursor, ParserContext context) {
        Declarations.optionalColon(cursor);

        HavingNode having = new HavingNode();

        having.setCondition(expression(cursor, context));

        return having;
    }

    private static ClauseNode parseWhere(TokenCursor cursor, ParserContext context) {
        Declarations.optionalColon(cursor);

        WhereNode where = new WhereNode();

        where.setCondition(expression(cursor, context));

        return where;
    }

    private static ClauseNode parseOrder(TokenCursor cursor, ParserContext context) {
        Declarations.optionalColon(cursor);

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
        Declarations.optionalColon(cursor);

        ColumnsNode columns = new ColumnsNode();

        do {
            Expression projection = expression(cursor, context);
            String     alias      = null;

            if (cursor.consumeIf(QueryToken.T_AS)) {
                // ⚠️ A keyword is a legal alias. `fetch: request.key as key` is the obvious thing to write,
                // and `key` is a word this grammar spends in a mapping — so the identifier rule refused the
                // natural case and left a message about an unexpected T_KEY to decode. Unambiguous here
                // because what follows an alias is a comma or the end of the clause, never a keyword.
                alias = Declarations.word(cursor);
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
