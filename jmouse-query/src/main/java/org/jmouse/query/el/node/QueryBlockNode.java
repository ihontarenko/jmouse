package org.jmouse.query.el.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.lexer.Token;
import org.jmouse.query.el.QueryParseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * What a {@code view} and a {@code function} have in common: a {@code { … }} holding clauses.
 *
 * <p>Clauses are held in the order they were written, and read back canonically.</p>
 *
 * <h2>⚠️ The set of clauses is open, and nothing here enumerates it</h2>
 *
 * <p>A clause states its own keyword, the capability a backend needs for it, where it belongs when the
 * block is written back out, and whether saying it twice is an {@code and}, two separate things, or a
 * mistake — all four on the clause itself, in its {@link ClauseKind}. So a clause added to the language costs a parser and a
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

    /**
     * The clauses, in the order they were written.
     *
     * <h2>⚠️ A list rather than a map keyed by keyword</h2>
     *
     * <p>A map made one keyword one clause, which is right for {@code fetch} and wrong for {@code join}:
     * two joins are two tables, and there is no one clause that says both. A list is the only shape that
     * holds all three of "once", "combined into one" and "kept apart" — see
     * {@link ClauseKind.Repetition}.</p>
     */
    private final List<ClauseNode> clauses = new ArrayList<>();

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
        ClauseNode existing = first(clause.keyword());

        if (existing == null || clause.kind().repetition() == ClauseKind.Repetition.MANY) {
            clauses.add(clause);
            return;
        }

        if (clause.kind().repetition() == ClauseKind.Repetition.ONCE) {
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
        ClauseNode existing = first(clause.keyword());

        if (existing == null || clause.kind().repetition() == ClauseKind.Repetition.MANY) {
            clauses.add(clause);
            return;
        }

        if (clause.kind().repetition() == ClauseKind.Repetition.ONCE) {
            throw new QueryParseException(
                    "this block already says '%s' once, and saying it twice is not an 'and'"
                            .formatted(clause.keyword()));
        }

        existing.merge(clause);
    }

    /**
     * Lays a clause over this block the way a request lays one over a STORED view.
     *
     * <h2>⚠️ Not {@link #addClause}, and the difference is who is speaking</h2>
     *
     * <p>Two clauses of one kind written in one text is a mistake, and {@code addClause} refuses it.
     * A saved view opened with {@code ?jmq:order=…} beside it is not that: the view was written once and
     * stored, the parameter is somebody saying <em>this time, sorted differently</em>, and refusing it
     * would make a stored question unusable the moment anybody wanted it narrowed.</p>
     *
     * <h2>⚠️ What a parameter DOES is read off the clause, never decided here</h2>
     *
     * <p>Whether the request narrows the view or replaces part of it is {@link ClauseKind.Repetition},
     * the same rule that decides what writing a clause twice means:</p>
     *
     * <table>
     *   <caption>How each kind overlays</caption>
     *   <tr><th>{@code MERGED}</th><td>{@code where}</td><td><b>narrows</b> — combined into what is there, an {@code and}</td></tr>
     *   <tr><th>{@code ONCE}</th><td>{@code order}, {@code fetch}, {@code limit}</td><td><b>replaces</b> that clause</td></tr>
     *   <tr><th>{@code MANY}</th><td>{@code join}</td><td><b>adds</b> another</td></tr>
     * </table>
     *
     * <p>So there is no second table of overlay rules to keep in step with the first, and a clause added
     * to the language overlays correctly the day it is registered, having said nothing about requests.</p>
     *
     * <p>⚠️ <strong>It mutates this block, so overlay a COPY of a stored view, never the stored one.</strong>
     * A view held in memory and overlaid per request would carry the first caller's narrowing into the
     * second caller's answer — which reads correctly and returns the wrong rows.</p>
     *
     * @param clause what the request said
     */
    public void overlay(ClauseNode clause) {
        ClauseKind.Repetition repetition = clause.kind().repetition();

        if (repetition == ClauseKind.Repetition.MANY) {
            clauses.add(clause);
            return;
        }

        ClauseNode existing = first(clause.keyword());

        if (existing == null) {
            clauses.add(clause);
            return;
        }

        if (repetition == ClauseKind.Repetition.MERGED) {
            existing.merge(clause);
            return;
        }

        clauses.set(clauses.indexOf(existing), clause);
    }

    private ClauseNode first(String keyword) {
        return clauses.stream().filter(clause -> clause.keyword().equals(keyword)).findFirst().orElse(null);
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
        // ⚠️ A STABLE sort, so two clauses of one kind keep the order they were written in. A join written
        // second is a join compiled second, and a statement whose tables swap places between runs is a
        // statement nobody can diff.
        return clauses.stream()
                .sorted(Comparator.comparingInt(clause -> clause.kind().order()))
                .toList();
    }

    /**
     * Every clause of one kind — what a clause written several times is read through.
     *
     * <p>⚠️ A clause whose repeats are kept apart has NO singular accessor, on purpose. One would return
     * the first of several and look like it worked.</p>
     */
    public <T extends ClauseNode> List<T> getClauses(Class<T> type) {
        return clauses.stream().filter(type::isInstance).map(type::cast).toList();
    }

    private <T extends ClauseNode> Optional<T> clause(String keyword, Class<T> type) {
        return Optional.ofNullable(first(keyword)).filter(type::isInstance).map(type::cast);
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
