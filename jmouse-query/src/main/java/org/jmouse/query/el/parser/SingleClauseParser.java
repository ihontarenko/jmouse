package org.jmouse.query.el.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.query.el.QueryParseException;
import org.jmouse.query.el.node.ClauseNode;

/**
 * One clause, read on its own — what {@code ?jmq:where=…} and {@code ?jmq:order=…} carry.
 *
 * <h2>⚠️ A parameter per clause, and the clause list is not enumerated anywhere</h2>
 *
 * <p>A request says what it wants one clause at a time, named by the language's own word. That costs no
 * separator — HTTP already separates parameters — and it is the only shape that could not be invented,
 * because the comma and the colon are <strong>inside</strong> jMQ: {@code in ['A', 'B']},
 * {@code fetch: a, b}, {@code view 'x':name}. Any {@code a:b,c:d} scheme cuts a list in half and parses
 * into something else without failing.</p>
 *
 * <p>⚠️ And because the clause is looked up in the registry rather than listed here, a clause added to
 * the language becomes a query parameter <strong>the day it is registered</strong>, with no edit to this
 * class and none to any controller.</p>
 *
 * <h2>⚠️ It refuses a parameter that says more than one clause</h2>
 *
 * <p>{@link OrderParser} already makes the argument this one generalises: a URL must never assemble a
 * document. Reading the clause directly is half of that; the other half is refusing what is left over.
 * A value of {@code issue.status == 'NEW' limit: 1} parses as a {@code where} and leaves {@code limit: 1}
 * behind — and a leftover silently dropped is a request that was partly honoured and said so nowhere.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class SingleClauseParser extends ExpressionParser {

    /**
     * Reads exactly one clause, keyword and all.
     *
     * @param cursor  positioned on the clause keyword
     * @param parent  where the clause is added
     * @param context the parser context
     */
    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Documents.skipBlankSpace(cursor);

        ClauseNode clause = ClauseParser.parse(cursor, context);

        Documents.skipBlankSpace(cursor);

        // ⚠️ End-of-line is not "something left over" — the lexer emits one at the end of every input, so
        // testing hasNext() alone refuses every well-formed parameter there is.
        if (cursor.hasNext() && !cursor.isCurrent(org.jmouse.el.lexer.BasicToken.T_EOL)) {
            throw new QueryParseException(
                    ("'%s' was given more than one clause — it reads to '%s' and then finds '%s'. "
                     + "One parameter says one thing; write the rest as their own parameters")
                            .formatted(clause.keyword(), clause.keyword(), cursor.peek().value()));
        }

        parent.add(clause);
    }
}
