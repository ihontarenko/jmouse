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
     * The statement this query compiles to — <strong>compiled, never executed</strong>.
     *
     * <h2>⚠️ It runs nothing, and that is the whole point of it existing here</h2>
     *
     * <p>Executing an arbitrary query on somebody's behalf needs a scope built from their session, paging
     * and a way to load whatever matched — none of which is the same in two products, and none of which
     * belongs to a shared controller. Compiling needs none of it. So the half that can honestly live in a
     * library is the half that answers <em>what would this ask the database</em>.</p>
     *
     * <h2>⚠️ What comes back is the query AS WRITTEN, without the product's own confinement</h2>
     *
     * <p>A listing adds its own scope before running — the projects this caller may browse, the workspace
     * they are in — and that fragment is supplied at the call site, not by the language. It is deliberately
     * absent here, because inventing one would be this module guessing at a product's authorization.</p>
     *
     * <p>So a reader must be told: this is the shape of the question, not the whole statement the listing
     * runs. Presenting it as the latter would teach somebody that their listing is unconfined.</p>
     *
     * @param source what it is asked of
     * @param filter the jMQ condition, or blank
     * @param order  the jMQ sort, or blank
     * @param values what the caller supplies by name
     * @return the statement and the values it would carry
     */
    public Explained explain(QuerySource source, String filter, String order, Map<String, Object> values) {
        return explain(source, filter, order, values, dialect());
    }

    /**
     * The same, written for a dialect the caller names rather than the one the connection reports.
     *
     * <h2>⚠️ A PREVIEW, and whoever shows it has to say so</h2>
     *
     * <p>Compiling for an engine this installation is not pointed at answers a real question — *what
     * would this look like on Postgres* — and produces a statement that is not what runs here. The
     * dialects differ in how an interval is written, and that is not a syntax error somebody notices: it
     * is a query that runs and answers about a different length of time.</p>
     *
     * @param dialect which engine to write for
     */
    public Explained explain(QuerySource source, String filter, String order,
                             Map<String, Object> values, Dialect dialect) {
        QueryEngine engine = QueryEngine.with(dialect).source(source).build();

        Fragment selected = compile(engine, source, filter, order, values)
                .select(Fragment.of("%s.%s".formatted(
                        source.target().alias(), dialect.quote(source.target().key()))));

        return new Explained(
                selected.sql(),
                selected.parameters().stream().map(String::valueOf).toList(),
                nameOf(dialect));
    }

    /**
     * The query as a TREE — one view holding whatever of the filter and the sort were given.
     *
     * <h2>⚠️ Exposed so that nothing assembles this a second time</h2>
     *
     * <p>Every destination starts from the same node: SQL for a vendor, a pipeline over rows, or jMQ
     * written back out. A caller that built its own view to hand to another translator would be a second
     * assembler of one shape — and the two agree until the day only one of them is taught a clause.</p>
     *
     * <p>⚠️ It is built from two PARSED halves and never from concatenated text: a filter is read by the
     * expression parser and a sort by the clause parser, so neither can carry a brace that restructures
     * the query.</p>
     *
     * @param source what the query is asked of
     * @param filter the jMQ condition, or blank
     * @param order  the jMQ sort, or blank
     * @return the view
     */
    public ViewNode compose(QuerySource source, String filter, String order) {
        return view(QueryEngine.with(dialect()).source(source).build().language(), source, filter, order);
    }

    /**
     * Which engine the statement was written for, as a person would say it.
     *
     * <p>⚠️ Not {@code getSimpleName()}. That put <em>MySqlDialect</em> on a badge next to a SQL
     * statement — a class name leaking onto a screen, where the reader wants the name of the database.
     * The suffix is this codebase's naming convention and is of no interest to anybody reading a query.</p>
     */
    /**
     * Which engine this installation is actually pointed at.
     *
     * <p>⚠️ So a caller can tell a preview from the real thing without compiling a second query to find
     * out — which is what it had to do, and which is a round trip to read a name.</p>
     */
    public String engine() {
        return nameOf(dialect());
    }

    private String nameOf(Dialect dialect) {
        String written = dialect.getClass().getSimpleName();

        return written.endsWith("Dialect")
                ? written.substring(0, written.length() - "Dialect".length()).toLowerCase()
                : written.toLowerCase();
    }

    /**
     * A compiled statement, and the values it would carry.
     *
     * <p>⚠️ Values are rendered as text for display only. They are the caller's own supplied values, so
     * showing them discloses nothing the caller did not type — but they are never re-parsed from this,
     * because a value that round-tripped through a string is a value whose type was decided twice.</p>
     *
     * @param sql        the statement, with placeholders
     * @param parameters what would be bound, in order
     * @param dialect    which engine it was written for — the interval syntax differs, and so do results
     */
    public record Explained(String sql, List<String> parameters, String dialect) {
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

        return engine.compile(view(engine.language(), source, filter, order), values);
    }

    /** The node assembly itself — see {@link #compose}, which is why this is one method. */
    private ViewNode view(QueryLanguage language, QuerySource source, String filter, String order) {
        ViewNode view = new ViewNode();

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

        return view;
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
