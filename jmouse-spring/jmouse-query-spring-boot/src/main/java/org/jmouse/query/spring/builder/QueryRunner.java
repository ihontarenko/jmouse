package org.jmouse.query.spring.builder;

import org.jmouse.el.node.Expression;
import org.jmouse.jdbc.dialect.Dialect;
import org.jmouse.jdbc.dialect.Dialects;
import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.el.node.ViewNode;
import org.jmouse.query.el.node.WhereNode;
import org.jmouse.query.sql.Fragment;
import org.jmouse.query.sql.QueryEngine;
import org.jmouse.query.sql.QuerySource;
import org.jmouse.query.sql.QueryTarget;
import org.jmouse.query.sql.ViewCompiler;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Running a jMQ filter over a declared source — the half that is not about any one product's rows.
 *
 * <h2>⚠️ Library-side the moment there was a second product, not a second caller</h2>
 *
 * <p>Every filterable listing asks the same four things: compile the filter and the sort into one view,
 * add the caller's scope, page the identifiers, count them honestly. What differs is only the
 * <strong>source</strong> and what the scope says — so those are arguments and the rest is written once.
 * Copied instead, the copies disagree first about paging, then about which condition the count was
 * over, and neither disagreement looks like a bug from the outside.</p>
 *
 * <h2>⚠️ Identifiers, not rows</h2>
 *
 * <p>It answers <em>which</em> rows match and <em>in what order</em>. Loading them stays with whichever
 * service already knows how to assemble one — a second read path would be a second place for the same
 * thing to look different.</p>
 *
 * <h2>⚠️ The scope is composed, never concatenated into the filter</h2>
 *
 * <p>The compiled condition and the caller's scope are two fragments {@code AND}-ed together, each
 * carrying its own bound values. So a filter arriving in a URL cannot reach another tenant, and cannot
 * {@code OR} its way past the scope — {@link ViewCompiler.CompiledQuery#and} parenthesises both sides.</p>
 *
 * <h2>⚠️ Paging is the database's, and the count is real</h2>
 *
 * <p>The filter becomes SQL, so {@code LIMIT} and {@code COUNT(*)} mean what they say. That is the whole
 * reason this exists rather than an in-memory pass: a bounded slice filtered in the application answers
 * correctly right up to the row that falls off the end of it, and then answers wrongly for ever.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource   dataSource;

    private volatile Dialect dialect;

    public QueryRunner(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    /** What matched, and how many there are in total. */
    public record Matches(List<String> identifiers, long total) {
    }

    /**
     * The rows of one source that satisfy a filter, in its order, for one page.
     *
     * @param source what is being queried
     * @param scope  the caller's own condition, built from the session — given the target and the
     *               dialect, because a scope has to quote and qualify exactly as the compiler does
     * @param filter the jMQ condition, or blank
     * @param order  the jMQ sort, or blank
     * @param values what the filter may name without having written it — {@code currentMember}, a tenant
     * @param offset how many rows to skip
     * @param limit  how many to return
     * @return the matching identifiers, and the total the filter matches
     */
    public Matches matching(
            QuerySource source,
            BiFunction<QueryTarget, Dialect, Fragment> scope,
            String filter,
            String order,
            Map<String, Object> values,
            long offset,
            int limit
    ) {
        QueryEngine engine = QueryEngine.with(dialect()).source(source).build();

        ViewCompiler.CompiledQuery scoped = compile(engine, source, filter, order, values)
                .and(scope.apply(source.target(), dialect()));

        Fragment identifiers = scoped.select(Fragment.of("%s.%s".formatted(
                source.target().alias(), dialect().quote(source.target().key()))));

        Fragment page = new Fragment(
                "%s %s".formatted(identifiers.sql(), dialect().limit(limit, offset)),
                identifiers.parameters());

        // ⚠️ Counted over the SAME condition and without the order: a count that re-derived the condition
        // could drift from the page it is counting, and sorting rows nobody looks at is work for nothing.
        Fragment total = scoped.orderedBy(Fragment.empty()).select(Fragment.of("COUNT(*)"));

        return new Matches(
                jdbcTemplate.queryForList(page.sql(), String.class, page.parameters().toArray()),
                jdbcTemplate.queryForObject(total.sql(), Long.class, total.parameters().toArray()));
    }

    /** Refuses a query without running it — what a save or a preview asks. */
    public void check(QuerySource source, String filter, String order, Map<String, Object> values) {
        compile(QueryEngine.with(dialect()).source(source).build(), source, filter, order, values);
    }

    /**
     * ⚠️ A filter and a sort are compiled through <strong>one view</strong>, never separately: two
     * compiles share no join aliases, so a sort over the same attribute would open a second join — and a
     * key that binds a value would be bound in a statement of its own, leaving nobody to decide which
     * parameter came first.
     *
     * <p>⚠️ Assembled from two PARSED halves, never from concatenated text: a filter is read by the
     * expression parser and a sort by the clause parser, so neither can carry a brace that restructures
     * the query.</p>
     */
    private ViewCompiler.CompiledQuery compile(
            QueryEngine engine,
            QuerySource source,
            String filter,
            String order,
            Map<String, Object> values) {

        QueryLanguage language = engine.language();
        ViewNode      view     = new ViewNode();

        view.setTarget(source.name());

        if (filter != null && !filter.isBlank()) {
            WhereNode  where     = new WhereNode();
            Expression condition = language.expression(filter);

            where.setCondition(condition);
            view.addClause(where);
        }

        if (order != null && !order.isBlank()) {
            view.addClause(language.order(order));
        }

        return engine.compile(view, values);
    }

    /**
     * ⚠️ Asked once, of the connection. The dialects differ in how an interval is written, and getting
     * that wrong is not a syntax error somebody notices — it is a query that runs and answers about a
     * different length of time.
     */
    public Dialect dialect() {
        Dialect resolved = dialect;

        if (resolved == null) {
            synchronized (this) {
                resolved = dialect;

                if (resolved == null) {
                    dialect = resolved = Dialects.of(dataSource);
                }
            }
        }

        return resolved;
    }
}
