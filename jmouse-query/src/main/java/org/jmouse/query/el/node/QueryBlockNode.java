package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.lexer.Token;
import org.jmouse.query.el.QueryParseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a {@code view} and a {@code function} have in common: a {@code { … }} holding clauses.
 *
 * <p>Clauses are kept keyed by their keyword rather than in a plain list, because two questions are
 * asked of them constantly and a list answers neither well: <em>has this block a {@code where}?</em>
 * and <em>was this clause written twice?</em></p>
 *
 * <p>⚠️ <strong>Insertion order is preserved and it matters.</strong> A person may write
 * {@code order} before {@code where} and the grammar allows it; {@code toSource()} then writes the
 * canonical order instead, so a document converges on one form the first time it is saved through a
 * builder — the same convergence {@code .jmp} arranges for its own optional prefixes. Keeping the
 * written order available means the choice of which to emit stays a decision rather than an
 * accident.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class QueryBlockNode extends AbstractExpression {

    /** The order clauses are written back in, whatever order they were read in. */
    private static final List<String> CANONICAL_ORDER =
            List.of(ColumnsNode.KEYWORD, WhereNode.KEYWORD, GroupNode.KEYWORD,
                    HavingNode.KEYWORD, OrderNode.KEYWORD);

    private final Map<String, ClauseNode> clauses = new LinkedHashMap<>();

    /**
     * Adds a clause, refusing a second one of the same kind.
     *
     * <p>⚠️ Refused rather than resolved. Two {@code where} lines read as though they ought to be
     * {@code and}-ed together; silently keeping the last is the kind of thing nobody notices until a
     * view returns rows it should not.</p>
     *
     * @param clause the clause to add
     * @param token  where it was written, for the refusal
     */
    public void addClause(ClauseNode clause, Token token) {
        if (clauses.containsKey(clause.keyword())) {
            throw QueryParseException.repeated(clause.keyword(), token);
        }

        clauses.put(clause.keyword(), clause);
    }

    /**
     * Adds a clause to a block nobody parsed.
     *
     * <h2>⚠️ For a block ASSEMBLED rather than read — and both need to exist</h2>
     *
     * <p>A builder screen composes a view from controls, and a request composes one from a filter and a
     * sort that arrived as two separate parameters. Neither has a token to point at, and neither should
     * be made to invent one: a position in a source file is exactly the thing they do not have.</p>
     *
     * <p>⚠️ It refuses a second clause of the same kind for the same reason the parsed path does — two
     * {@code where} clauses read as though they ought to be {@code and}-ed together, and quietly keeping
     * the last is how a view returns rows it should not.</p>
     *
     * @param clause the clause to add
     */
    public void addClause(ClauseNode clause) {
        if (clauses.containsKey(clause.keyword())) {
            throw new QueryParseException(
                    "this block already says '%s' once, and saying it twice is not an 'and'"
                            .formatted(clause.keyword()));
        }

        clauses.put(clause.keyword(), clause);
    }

    public Optional<WhereNode> getWhere() {
        return clause(WhereNode.KEYWORD, WhereNode.class);
    }

    public Optional<OrderNode> getOrder() {
        return clause(OrderNode.KEYWORD, OrderNode.class);
    }

    public Optional<ColumnsNode> getColumns() {
        return clause(ColumnsNode.KEYWORD, ColumnsNode.class);
    }

    public Optional<GroupNode> getGroup() {
        return clause(GroupNode.KEYWORD, GroupNode.class);
    }

    public Optional<HavingNode> getHaving() {
        return clause(HavingNode.KEYWORD, HavingNode.class);
    }

    /**
     * Whether this block gathers rows.
     *
     * <p>⚠️ Asked by anything that reads the result, because a grouped query returns TUPLES rather than
     * rows of the underlying thing — and paging over one counts groups, not rows.</p>
     */
    public boolean isGrouped() {
        return getGroup().isPresent();
    }

    /**
     * Every clause this block holds, in the order it was written.
     *
     * @return the clauses as read
     */
    public List<ClauseNode> getClauses() {
        return List.copyOf(clauses.values());
    }

    private <T extends ClauseNode> Optional<T> clause(String keyword, Class<T> type) {
        return Optional.ofNullable(clauses.get(keyword)).filter(type::isInstance).map(type::cast);
    }

    /**
     * Writes the block's body — the clauses, canonically ordered, one per line and indented.
     *
     * @param indent the indentation each clause line carries
     * @return the body, without the surrounding braces
     */
    protected String clausesToSource(String indent) {
        List<String> lines = new ArrayList<>();

        for (String keyword : CANONICAL_ORDER) {
            ClauseNode clause = clauses.get(keyword);

            if (clause != null) {
                lines.add(indent + clause.toSource());
            }
        }

        return String.join("\n", lines);
    }
}
