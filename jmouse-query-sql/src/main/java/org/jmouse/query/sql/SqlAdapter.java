package org.jmouse.query.sql;

import org.jmouse.jdbc.dialect.Dialect;
import org.jmouse.query.adapter.Capabilities;
import org.jmouse.query.adapter.QueryAdapter;
import org.jmouse.query.el.node.QueryBlockNode;
import org.jmouse.query.schema.QueryChecker;

import java.util.Map;

/**
 * The SQL backend, on the same seam every other backend sits on.
 *
 * <h2>⚠️ Why this exists when {@link QueryEngine} already compiles SQL</h2>
 *
 * <p>{@link QueryAdapter} declared the shape a backend takes, and until now only the in-memory one
 * actually took it — the SQL path ran alongside the interface rather than through it. That made the
 * epic's promise, <em>one language, several backends</em>, true of the language and not of the thing a
 * product holds: a configured {@code QueryEngine} is SQL and nothing else, with no
 * {@code QueryAdapter<?>} a caller could be handed instead.</p>
 *
 * <p>So a product that wants to be backend-agnostic — a builder's preview running in memory and the
 * same document running against a database, a report that may one day come from somewhere else — asks
 * for an adapter, and this is the one that answers in SQL.</p>
 *
 * <h2>⚠️ It is not a replacement for the facade</h2>
 *
 * <p>{@link QueryEngine} stays the ergonomic path, and it is the richer one: {@code compileFilter} for a
 * bare expression, {@link ViewCompiler.CompiledQuery} <em>parts</em> for row-level scoping and counts.
 * An adapter's contract is deliberately narrower — a block in, a compiled form out — because that is
 * the most every backend can promise. Nothing that uses the facade needs to change.</p>
 *
 * <h2>⚠️ Capabilities are declared, and honestly</h2>
 *
 * <p>Everything: a relational database filters, sorts, projects, groups, joins, converts and knows the
 * clock. The memory adapter declares less and refuses the rest by name, which is the entire point of
 * {@link Capabilities} — a backend that quietly dropped a {@code group} and returned ungrouped rows
 * would be the failure this whole cluster was built to prevent.</p>
 *
 * <h2>⚠️ And it checks before it compiles, like every other path</h2>
 *
 * <p>There is no method here that compiles without the schema check first. Not as a convention — as the
 * only available shape, exactly as in {@link QueryEngine}. A backend reached through an interface is not
 * a backend allowed a weaker guarantee.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class SqlAdapter implements QueryAdapter<Fragment> {

    private static final Capabilities CAPABILITIES = Capabilities.everything("sql");

    private final QuerySource source;
    private final Dialect     dialect;

    public SqlAdapter(QuerySource source, Dialect dialect) {
        this.source = source;
        this.dialect = dialect;
    }

    @Override
    public Capabilities capabilities() {
        return CAPABILITIES;
    }

    /**
     * Compiles a block into a runnable statement and its bound values.
     *
     * @param block a view or a function body
     * @return the statement, with its parameters
     */
    @Override
    public Fragment compile(QueryBlockNode block) {
        return parts(block).select();
    }

    /**
     * The compiled parts, for a caller that wants to assemble the statement itself — a count, a tenant
     * condition composed onto the {@code WHERE}, a screen with its own projection.
     *
     * <p>⚠️ Offered here as well as on the facade so that reaching a backend through the interface does
     * not cost a caller the seam that keeps the facade from becoming a wall.</p>
     *
     * @param block a view or a function body
     * @return the parts
     */
    public ViewCompiler.CompiledQuery parts(QueryBlockNode block) {
        return parts(block, Map.of());
    }

    /**
     * The compiled parts, against values the caller supplies.
     *
     * @param block  a view or a function body
     * @param values what the caller supplies by name — {@code currentMember}, a tenant
     * @return the parts
     */
    public ViewCompiler.CompiledQuery parts(QueryBlockNode block, Map<String, Object> values) {
        requireSupport(block);

        new QueryChecker(source.schema(), values.keySet()).check(block);

        return new ViewCompiler(source.mapping(), source.membership())
                .compile(block, dialect, source.schema(), source.target(), values);
    }

    /** What this adapter is configured over. */
    public QuerySource source() {
        return source;
    }
}
