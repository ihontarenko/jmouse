package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.lexer.Token;
import org.jmouse.query.el.QueryParseException;

import java.util.ArrayList;
import java.util.Comparator;
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
 * <h2>⚠️ The set of clauses is open, and nothing here enumerates it</h2>
 *
 * <p>A clause states its own keyword, the capability a backend needs for it, where it belongs when the
 * block is written back out, and whether saying it twice is an {@code and} or a mistake — all four on
 * the clause itself, in its {@link ClauseKind}. So a clause added to the language costs a parser and a
 * capability, and no edit to this class or to any backend.</p>
 *
 * <p>⚠️ That is not tidiness. This class used to keep a hard-coded rendering order of the five clauses
 * that existed, which meant a sixth would parse correctly and then <strong>disappear</strong> on the way
 * back out — a saved query quietly losing a clause the first time somebody edited it.</p>
 *
 * <p>A person may write {@code order} before {@code where} and the grammar allows it; {@code toSource()}
 * writes the canonical order instead, so a document converges on one form the first time it is saved
 * through a builder — the same convergence {@code .jmp} arranges for its own optional prefixes.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class QueryBlockNode extends AbstractExpression {

    private final Map<String, ClauseNode> clauses = new LinkedHashMap<>();

    /**
     * Adds a clause — combining it with the one already there when the clause says it may be repeated,
     * and refusing it when it does not.
     *
     * <p>⚠️ Both halves matter. Two {@code where} lines read as though they ought to be {@code and}-ed
     * together, and now they are — a product composing a scope onto somebody else's filter no longer has
     * to take the expression apart to do it. Two {@code fetch} lines read as a mistake, and stay
     * refused: silently keeping the last is the kind of thing nobody notices until a view returns rows
     * it should not.</p>
     *
     * <p>Which of the two a clause gets is the clause's own declaration, not a list kept here — see
     * {@link ClauseKind}.</p>
     *
     * @param clause the clause to add
     * @param token  where it was written, for the refusal
     */
    public void addClause(ClauseNode clause, Token token) {
        ClauseNode existing = clauses.get(clause.keyword());

        if (existing == null) {
            clauses.put(clause.keyword(), clause);
            return;
        }

        if (!clause.kind().repeatable()) {
            throw QueryParseException.repeated(clause.keyword(), token);
        }

        existing.merge(clause);
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
     * <p>⚠️ It treats a repeat exactly as the parsed path does — combined where the clause allows it,
     * refused where it does not. An assembled block is not a block held to a weaker rule.</p>
     *
     * @param clause the clause to add
     */
    public void addClause(ClauseNode clause) {
        ClauseNode existing = clauses.get(clause.keyword());

        if (existing == null) {
            clauses.put(clause.keyword(), clause);
            return;
        }

        if (!clause.kind().repeatable()) {
            throw new QueryParseException(
                    "this block already says '%s' once, and saying it twice is not an 'and'"
                            .formatted(clause.keyword()));
        }

        existing.merge(clause);
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
     * Every clause this block carries, in the order it is written back out.
     *
     * <h2>⚠️ Canonical, and derived from the clauses rather than from a list kept here</h2>
     *
     * <p>Each clause states where it sits ({@link ClauseKind#order()}), so a clause the language gains
     * tomorrow takes its place without this class being edited — and, more to the point, without
     * <strong>vanishing on un-parse</strong>, which is what a hard-coded rendering order did to anything
     * it had never heard of.</p>
     *
     * <p>A person may write {@code order} before {@code where} and the grammar allows it; what comes
     * back is the canonical form, so a document converges on one spelling the first time it is saved
     * through a builder.</p>
     */
    public List<ClauseNode> getClauses() {
        return clauses.values().stream()
                .sorted(Comparator.comparingInt((ClauseNode clause) -> clause.kind().order())
                                .thenComparing(ClauseNode::keyword))
                .toList();
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

        for (ClauseNode clause : getClauses()) {
            lines.add(indent + clause.toSource());
        }

        return String.join("\n", lines);
    }
}
