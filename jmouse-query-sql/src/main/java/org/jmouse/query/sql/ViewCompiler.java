package org.jmouse.query.sql;

import org.jmouse.jdbc.dialect.Dialect;
import org.jmouse.el.node.Expression;
import org.jmouse.query.el.node.ColumnsNode;
import org.jmouse.query.el.node.OrderNode;
import org.jmouse.query.el.node.QueryBlockNode;
import org.jmouse.query.schema.QuerySchema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compiles a whole view — its joins, its {@code WHERE} and its {@code ORDER BY} — into one statement's
 * worth of SQL with the parameters in binding order.
 *
 * <h2>⚠️ Assembly order is the point of this class</h2>
 *
 * <p>The three parts are <em>discovered</em> in one order and <em>written</em> in another. Joins are
 * found while walking the {@code where}, yet they belong before it; an {@code order} is compiled last
 * and belongs last. Parameters bind by position, so text and values have to be assembled together —
 * which is why each part is a {@link Fragment} rather than a string, and why they are joined with
 * {@link Fragment#then} instead of concatenated.</p>
 *
 * <p>⚠️ Getting this wrong does not raise. The statement runs, binds each value to the wrong placeholder,
 * and answers a question nobody asked.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ViewCompiler {

    private final AttributeMapping  mapping;
    private final MembershipMapping membership;

    public ViewCompiler(AttributeMapping mapping) {
        this(mapping, null);
    }

    /**
     * @param membership how this source answers a question about a collection, or {@code null} when it
     *                   declares none
     */
    public ViewCompiler(AttributeMapping mapping, MembershipMapping membership) {
        this.mapping = mapping;
        this.membership = membership;
    }

    /**
     * Compiles a view or a function body.
     *
     * @param block   the parsed block
     * @param dialect which database this is for
     * @param schema  what may be filtered
     * @param target  the table the document named after {@code on}
     * @return the parts, each carrying its own values
     */
    public CompiledQuery compile(QueryBlockNode block, Dialect dialect, QuerySchema schema, QueryTarget target) {
        return compile(block, dialect, schema, target, Map.of());
    }

    /**
     * Compiles a view or a function body against values the caller supplies.
     *
     * @param block   the parsed block
     * @param dialect which database this is for
     * @param schema  what may be filtered
     * @param target  the table the document named after {@code on}
     * @param values  what the caller supplies by name — {@code currentMember}, a tenant, a threshold
     * @return the parts, each carrying its own values
     */
    public CompiledQuery compile(QueryBlockNode block, Dialect dialect, QuerySchema schema, QueryTarget target,
                                 Map<String, Object> values) {

        SqlContext  context  = new SqlContext(
                dialect, schema, target, SqlContext.DEFAULT_ALIAS_PREFIX, Instant.now(), values);
        SqlCompiler compiler = new SqlCompiler(mapping, membership, context);

        // ⚠️ The `where` is compiled FIRST even though it is written after the joins: walking it is what
        // discovers which joins are needed at all.
        Fragment where = block.getWhere()
                .map(clause -> compiler.compile(clause.getCondition()))
                .orElse(Fragment.empty());

        Fragment columns = columns(block, compiler);
        Fragment group = group(block, compiler);
        Fragment having = block.getHaving()
                .map(clause -> compiler.compile(clause.getCondition()))
                .orElse(Fragment.empty());
        Fragment order = order(block, compiler);

        // ⚠️ The FROM is rendered here, where the dialect is known. A target rendering its own would have
        // to render it unquoted, and a statement that quotes some identifiers and not others works right
        // up until a table is called `order`.
        return new CompiledQuery(context.from(), columns, context.joins(), where, group, having, order,
                block.isGrouped());
    }

    /**
     * The {@code columns} clause.
     *
     * <p>⚠️ <strong>This was parsed, checked and then silently dropped.</strong> A projection the
     * document asked for and the statement ignored is worse than one that is not supported: the query
     * runs, returns every column, and nothing says the request was thrown away.</p>
     *
     * <p>An alias is quoted, because a person naming a column {@code "total value"} is exactly who uses
     * this clause.</p>
     */
    private Fragment columns(QueryBlockNode block, SqlCompiler compiler) {
        if (block.getColumns().isEmpty()) {
            return Fragment.empty();
        }

        List<String> written = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (ColumnsNode.Projection projection : block.getColumns().get().getProjections()) {
            Fragment compiled = compiler.compile(projection.expression());
            String alias = projection.alias();

            written.add(alias == null
                    ? compiled.sql()
                    : "%s AS %s".formatted(compiled.sql(), compiler.dialect().quote(alias)));
            values.addAll(compiled.parameters());
        }

        return new Fragment(String.join(", ", written), values);
    }

    private Fragment group(QueryBlockNode block, SqlCompiler compiler) {
        if (block.getGroup().isEmpty()) {
            return Fragment.empty();
        }

        List<String> written = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (Expression key : block.getGroup().get().getKeys()) {
            Fragment compiled = compiler.compile(key);

            written.add(compiled.sql());
            values.addAll(compiled.parameters());
        }

        return new Fragment(String.join(", ", written), values);
    }

    private Fragment order(QueryBlockNode block, SqlCompiler compiler) {
        if (block.getOrder().isEmpty()) {
            return Fragment.empty();
        }

        List<String> written = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (OrderNode.Key key : block.getOrder().get().getKeys()) {
            Fragment compiled = compiler.compile(key.expression());
            String direction = key.direction() == OrderNode.Direction.DESCENDING ? " DESC" : " ASC";

            written.add(compiled.sql() + direction);

            // ⚠️ Collected directly rather than through Fragment#then. That method treats a fragment with
            // blank SQL as nothing at all and returns the left side untouched — which is right for
            // assembling clauses and quietly WRONG here, where a sort key's text and its values are added
            // in separate steps. A key that binds a value would have had its value dropped, and the
            // remaining parameters would each shift one place: a query that runs and answers wrongly.
            values.addAll(compiled.parameters());
        }

        return new Fragment(String.join(", ", written), values);
    }

    /**
     * A compiled view, in the order the parts appear in a statement.
     *
     * <p>Handed back as parts rather than as one finished statement because the caller owns the
     * {@code SELECT} and the {@code FROM} — it knows what it is selecting and from where, and this does
     * not.</p>
     *
     * @param from    the target's {@code FROM} clause, already quoted — {@code `entries` e}
     * @param columns the projection the document asked for, or empty
     * @param joins   the joins the filter needed, or empty
     * @param where   the condition, without the {@code WHERE} keyword, or empty
     * @param group   the grouping keys, without the {@code GROUP BY} keyword, or empty
     * @param having  the group condition, without the {@code HAVING} keyword, or empty
     * @param orderBy the sort keys, without the {@code ORDER BY} keyword, or empty
     * @param grouped ⚠️ whether a row of this result is a <strong>tuple</strong> rather than a row of the
     *                underlying thing. Reported rather than left to be noticed: a screen, an export and
     *                an agent all read the result differently depending on it, and ⚠️ paging over a
     *                grouped query counts <em>groups</em> — a count that is otherwise entirely plausible
     *                and wrong.
     */
    public record CompiledQuery(String from, Fragment columns, Fragment joins, Fragment where,
                                Fragment group, Fragment having, Fragment orderBy, boolean grouped) {

        /**
         * The same query with something added to its condition — row-level scoping, a tenant, a
         * soft-delete filter.
         *
         * <p>⚠️ <strong>Offered because the alternative was rebuilding the record by hand</strong>, and a
         * caller doing that has to name eight fields in the right order. Miss one and a projection or a
         * {@code GROUP BY} silently disappears from the statement; swap two and the parameters bind to
         * the wrong placeholders. Neither raises.</p>
         *
         * <p>The condition is combined with {@link Fragment#and}, so both sides are parenthesised and an
         * {@code OR} inside either cannot swallow the other.</p>
         *
         * @param condition what to add
         * @return the same query, narrowed
         */
        public CompiledQuery and(Fragment condition) {
            return new CompiledQuery(from, columns, joins, where.and(condition), group, having, orderBy,
                    grouped);
        }

        /**
         * The same query sorted differently — for a screen that owns its own ordering.
         *
         * @param keys the sort keys, without the {@code ORDER BY} keyword
         * @return the same query, re-sorted
         */
        public CompiledQuery orderedBy(Fragment keys) {
            return new CompiledQuery(from, columns, joins, where, group, having, keys, grouped);
        }

        /**
         * The whole statement, selecting what the document asked for.
         *
         * <p>⚠️ The projection's own values come <strong>first</strong>, because {@code SELECT} is
         * written before {@code FROM}. A computed column binding a value — {@code price * ? as retail} —
         * therefore binds ahead of every join and every condition, and assembling this by hand is
         * precisely where that gets reversed.</p>
         *
         * @return a runnable statement and its values
         */
        public Fragment select() {
            return select(columns.isEmpty() ? Fragment.of("*") : columns);
        }

        /**
         * The whole statement, selecting something the caller chose instead.
         *
         * <p>Useful for a count, or for a screen that always fetches the same columns whatever the
         * document says.</p>
         *
         * @param selection what to select
         * @return a runnable statement and its values
         */
        public Fragment select(Fragment selection) {
            return Fragment.of("SELECT ").then(selection, "")
                    .then(Fragment.of("FROM %s".formatted(from)), " ")
                    .then(tail(), " ");
        }

        /**
         * The three parts as one fragment, in statement order, ready to follow a {@code FROM}.
         *
         * @return joins, then {@code WHERE …}, then {@code ORDER BY …}
         */
        public Fragment tail() {
            Fragment assembled = joins;

            if (!where.isEmpty()) {
                assembled = assembled.then(where.wrap("WHERE ", ""), " ");
            }

            // ⚠️ GROUP BY then HAVING then ORDER BY — statement order, which is also binding order.
            if (!group.isEmpty()) {
                assembled = assembled.then(group.wrap("GROUP BY ", ""), " ");
            }

            if (!having.isEmpty()) {
                assembled = assembled.then(having.wrap("HAVING ", ""), " ");
            }

            if (!orderBy.isEmpty()) {
                assembled = assembled.then(orderBy.wrap("ORDER BY ", ""), " ");
            }

            return assembled;
        }
    }
}
