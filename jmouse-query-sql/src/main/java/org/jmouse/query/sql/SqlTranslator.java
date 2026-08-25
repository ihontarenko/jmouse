package org.jmouse.query.sql;

import org.jmouse.jdbc.dialect.Dialect;
import org.jmouse.query.translate.Capabilities;
import org.jmouse.query.translate.Bindings;
import org.jmouse.query.translate.Capability;
import org.jmouse.query.translate.DeclaredValues;
import org.jmouse.query.translate.Translator;
import org.jmouse.query.translate.UnsupportedQueryException;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.query.el.node.QueryBlockNode;
import org.jmouse.query.el.node.WhereNode;
import org.jmouse.query.schema.QueryChecker;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The SQL backend, on the same seam every other backend sits on.
 *
 * <h2>⚠️ Why this exists when {@link QueryEngine} already compiles SQL</h2>
 *
 * <p>{@link Translator} declares the shape a backend takes, and until now only the in-memory one
 * actually took it — the SQL path ran alongside the interface rather than through it. That made the
 * epic's promise, <em>one language, several backends</em>, true of the language and not of the thing a
 * product holds: a configured {@code QueryEngine} is SQL and nothing else, with no
 * {@code Translator<?>} a caller could be handed instead.</p>
 *
 * <p>So a product that wants to be backend-agnostic — a builder's preview running in memory and the
 * same document running against a database, a report that may one day come from somewhere else — asks
 * for an adapter, and this is the one that answers in SQL.</p>
 *
 * <h2>⚠️ It is not a replacement for the facade</h2>
 *
 * <p>{@link QueryEngine} stays the ergonomic path, and it is the richer one: {@code compileFilter} for a
 * bare expression, {@link ViewCompiler.CompiledQuery} <em>parts</em> for row-level scoping and counts.
 * A translator's contract is deliberately narrower — a block in, a compiled form out — because that is
 * the most every backend can promise. Nothing that uses the facade needs to change.</p>
 *
 * <h2>⚠️ Capabilities are declared, and honestly</h2>
 *
 * <p>Everything: a relational database filters, sorts, projects, groups, joins, converts and knows the
 * clock. The row translator declares less and refuses the rest by name, which is the entire point of
 * {@link Capabilities} — a translator that quietly dropped a {@code group} and returned ungrouped rows
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
public class SqlTranslator implements Translator<Fragment> {

    private static final Capabilities CAPABILITIES = Capabilities.everything("sql");

    private final QuerySource source;
    private final Dialect     dialect;

    /**
     * How a named view is resolved into the statement it stands for.
     *
     * <p>⚠️ A resolver rather than a map, so an inner view is compiled only when it is actually named. A
     * document declaring twenty views would otherwise compile all twenty to answer a query mentioning
     * one.</p>
     */
    private final Function<String, Optional<Fragment>> subqueries;

    /** The names this translator can resolve into an inner statement. */
    private final Set<String> views;

    public SqlTranslator(QuerySource source, Dialect dialect) {
        this(source, dialect, name -> Optional.empty());
    }

    /**
     * ⚠️ The two-argument constructor kept meaning what it meant — a translator that knows no views, and
     * therefore refuses a name that stands for one exactly as it always did.
     */
    public SqlTranslator(QuerySource source, Dialect dialect,
                         Function<String, Optional<Fragment>> subqueries) {
        this(source, dialect, subqueries, Set.of());
    }

    /**
     * @param views the names that may stand in for a set — the ones the resolver can actually find
     */
    public SqlTranslator(QuerySource source, Dialect dialect,
                         Function<String, Optional<Fragment>> subqueries, Set<String> views) {
        this.source = source;
        this.dialect = dialect;
        this.subqueries = subqueries;
        this.views = Set.copyOf(views);
    }

    @Override
    public Capabilities capabilities() {
        return CAPABILITIES;
    }

    /**
     * Translates a whole block into a runnable statement, or one condition into a predicate.
     *
     * <h2>⚠️ A {@code where} on its own comes back as a PREDICATE, not as a {@code SELECT}</h2>
     *
     * <p>That is the half of this seam a product actually reaches for. Somebody holds a statement they
     * built with their own tooling — a repository method, a criteria query, a report — and wants one
     * condition a person composed spliced into it. Before this existed the only way was to take the
     * finished {@code SELECT *} and rewrite it with a regular expression, which puts a parser for this
     * compiler's own output in somebody else's module.</p>
     *
     * <p>⚠️ And it comes back as a {@link Fragment}: text <strong>and its bound values</strong>. A
     * predicate handed over as a bare string would have to be pasted, and pasting is the thing this
     * whole design refuses to do.</p>
     *
     * @param node     a view, a function body, a {@code where} clause, or a bare condition
     * @param bindings what the caller supplies by name — {@code currentMember}, a tenant
     * @return a statement for a block, a predicate for a condition
     */
    @Override
    public Fragment translate(Node node, Bindings bindings) {
        if (node instanceof QueryBlockNode block) {
            return parts(block, bindings.asMap()).select();
        }

        if (node instanceof WhereNode clause) {
            return predicate(clause.getCondition(), bindings);
        }

        if (node instanceof Expression condition) {
            return predicate(condition, bindings);
        }

        throw new UnsupportedQueryException(
                "the '%s' translator was handed a %s; it reads a block, a 'where' or a condition"
                        .formatted(CAPABILITIES.translator(), node.getClass().getSimpleName()));
    }

    /**
     * One condition, compiled to the predicate it is — no projection, no {@code FROM}, no statement.
     *
     * <p>⚠️ Checked with {@code checkCondition} rather than {@code check}: this text chooses ROWS, so it
     * is refused an aggregate exactly as a {@code where} clause is.</p>
     *
     * @param condition the expression
     * @param bindings  what the caller supplies by name
     * @return the predicate and its bound values
     */
    public Fragment predicate(Expression condition, Bindings bindings) {
        capabilities().require(Capability.FILTER, "where");

        new QueryChecker(source.schema(), bindings.names()).checkCondition(condition);

        SqlContext context = new SqlContext(dialect, source.schema(), source.target(),
                SqlContext.DEFAULT_ALIAS_PREFIX, Instant.now(), bindings.asMap());

        context.subqueries(subqueries);

        return new SqlCompiler(source.mapping(), source.membership(), context).compile(condition);
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

        // ⚠️ Omissions refused BEFORE the check, so a declared name reaches the compiler with a value, or
        // with the default that stands in for it, or not at all. Binding a null for something a view said
        // it needs is the failure this step exists to make impossible.
        DeclaredValues.Declared declared = DeclaredValues.resolve(block, Bindings.of(values));
        Map<String, Object>     supplied = declared.asMap();

        new QueryChecker(source.schema(),
                QueryEngine.allowedNames(block, supplied.keySet(), viewNames())).check(block);

        return new ViewCompiler(source.mapping(), source.membership())
                .defaults(declared.defaults())
                .compile(block, dialect, source.schema(), source.target(), supplied, subqueries);
    }

    /**
     * ⚠️ Only names this translator can actually RESOLVE. A view name allowed by the checker and then not
     * found by the resolver would compile into a bound null and return no rows — success, and wrong.
     */
    private Set<String> viewNames() {
        return views;
    }

    /** What this adapter is configured over. */
    public QuerySource source() {
        return source;
    }
}
