package org.jmouse.query.sql;

import org.jmouse.el.node.Expression;
import org.jmouse.jdbc.dialect.Dialect;
import org.jmouse.query.schema.QuerySchema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a compile pass carries: the dialect, the schema, the target, the aliases handed out and the joins
 * collected.
 *
 * <h2>⚠️ What it deliberately does NOT carry: bound values</h2>
 *
 * <p>An earlier version accumulated parameters here and drained them per clause, and it had a real bug —
 * a sort key's values were appended in a step separate from its text, so a key that bound a value
 * dropped it and shifted every remaining parameter one place. The statement ran and answered something
 * else.</p>
 *
 * <p>Values now travel inside the {@link Fragment} each node returns, so text and values cannot fall out
 * of step: a parent combines children in the order it writes them. There is no accumulator to get wrong.</p>
 *
 * <h2>⚠️ Joins are still collected, and they have to be</h2>
 *
 * <p>A join is discovered while walking a {@code where} and belongs in the SQL <em>before</em> it. That
 * is a genuine reordering rather than an accumulator, so the joins keep their own fragments and assembly
 * puts them ahead of the clause that needed them — see {@link ViewCompiler}.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class SqlContext {

    /**
     * What a generated join alias starts with — {@code j1}, {@code j2}.
     *
     * <p>⚠️ Named because two places allocate a context, and a second spelling would produce two alias
     * spaces that look alike: {@code j1} from one and {@code j1} from the other, correlating the wrong
     * subquery. A caller composing fragments from two contexts passes a different prefix ON PURPOSE.</p>
     */
    public static final String DEFAULT_ALIAS_PREFIX = "j";

    private final Dialect     dialect;
    private final QuerySchema schema;
    private final QueryTarget target;
    private final String      aliasPrefix;
    private final Instant     now;

    /** What the caller supplied by name, bound rather than read off a row. */
    private final Map<String, Object> values;

    /**
     * The inner {@code SELECT} a named view stands for — {@code x in supportPeople}.
     *
     * <p>⚠️ A resolver rather than a map of fragments, because an inner view is only worth compiling if it
     * is actually named. A document declaring twenty views would otherwise compile all twenty to answer a
     * query that mentions one.</p>
     */
    private java.util.function.Function<String, Optional<Fragment>> subqueries = name -> Optional.empty();

    /** How a named view is resolved into the statement it stands for. */
    public void subqueries(java.util.function.Function<String, Optional<Fragment>> resolver) {
        this.subqueries = resolver;
    }

    /** The inner statement this name stands for, where it names a declared view. */
    public Optional<Fragment> subquery(String name) {
        return subqueries.apply(name);
    }

    /**
     * The expression standing in for a declared name the caller left out — its default.
     *
     * <h2>⚠️ A tree, not a value, and that is the whole point</h2>
     *
     * <p>A default may say {@code now() - days(30)}. Worked out separately it would need a clock of its
     * own, and this context binds one moment for the whole statement precisely so that two clauses cannot
     * disagree about when "now" was. Compiled where the name stands, the default shares that moment for
     * free — and may use every function the query itself may use, product-contributed ones included.</p>
     */
    private Map<String, Expression> defaults = Map.of();

    /** What each unsupplied declared name compiles as. */
    public void defaults(Map<String, Expression> defaults) {
        this.defaults = Map.copyOf(defaults);
    }

    /** Whether this name is a declared one standing on its default rather than on a supplied value. */
    public boolean hasDefault(String name) {
        return defaults.containsKey(name);
    }

    /** The expression this name stands for. */
    public Expression defaultOf(String name) {
        return defaults.get(name);
    }

    /** One alias per key, in the order they were first asked for. */
    private final Map<String, String> aliases = new LinkedHashMap<>();

    /** Which aliases already have a join, so registering one twice is a no-op. */
    private final Set<String> joined = new LinkedHashSet<>();

    /** The joins, in registration order, each already carrying its own bound values. */
    private final List<Fragment> joins = new ArrayList<>();

    public SqlContext(Dialect dialect, QuerySchema schema, QueryTarget target) {
        this(dialect, schema, target, DEFAULT_ALIAS_PREFIX);
    }

    public SqlContext(Dialect dialect, QuerySchema schema, QueryTarget target, String aliasPrefix) {
        this(dialect, schema, target, aliasPrefix, Instant.now());
    }

    /**
     * @param now what {@code now()} means for this compile
     */
    public SqlContext(Dialect dialect, QuerySchema schema, QueryTarget target, String aliasPrefix, Instant now) {
        this(dialect, schema, target, aliasPrefix, now, Map.of());
    }

    /**
     * @param values what the caller supplies by name — {@code currentMember}, a tenant, a threshold
     */
    public SqlContext(Dialect dialect, QuerySchema schema, QueryTarget target, String aliasPrefix,
                      Instant now, Map<String, Object> values) {

        // ⚠️ Refused rather than bound. `= ?` with a null value is never true in SQL — not even for a null
        // column — so a query given a null value would run, match nothing, and say nothing about why. The
        // question somebody meant to ask is `is null`, and it is a different question.
        values.forEach((name, value) -> {
            if (value == null) {
                throw new SqlCompileException(
                        ("'%s' was supplied as nothing at all, and a comparison with nothing is never "
                         + "true in SQL — so this query would quietly match no rows. Ask 'is null' if "
                         + "that is the question").formatted(name));
            }
        });

        this.dialect = dialect;
        this.schema = schema;
        this.target = target;
        this.aliasPrefix = aliasPrefix;
        this.now = now;
        this.values = Map.copyOf(values);
    }

    /**
     * Whether this name is something the caller supplied rather than something the row holds.
     *
     * <h2>⚠️ Bound, never spliced — which is the whole reason values are a mechanism</h2>
     *
     * <p>{@code issue.assignee == currentMember} is the shape every product writes, and the tempting
     * implementation is to paste the value into the text before parsing it. That is a concatenated query:
     * a display name with an apostrophe breaks it, and a chosen one rewrites it. Here the name survives
     * parsing as a name and becomes a {@code ?} at the very end.</p>
     *
     * <p>⚠️ It also means the caller decides what a value is. Nothing a query WRITES can invent one, so
     * an expression out of a URL cannot reach a value the server did not hand over.</p>
     */
    public boolean hasValue(String name) {
        return values.containsKey(name);
    }

    /** What the caller supplied under this name. */
    public Object value(String name) {
        return values.get(name);
    }

    /** Every name the caller supplied. */
    public Map<String, Object> values() {
        return values;
    }

    /**
     * The moment this query was compiled.
     *
     * <h2>⚠️ Taken ONCE, and bound rather than left to the database</h2>
     *
     * <p>Two clauses each calling {@code now()} must agree, or a query compiled across a second boundary
     * returns rows satisfying neither of them. Fixing it here rather than emitting the database's own
     * {@code NOW()} settles a second thing too: the application's clock and the server's can differ, and
     * nothing in a result would say which one answered.</p>
     *
     * <p>⚠️ It is a constructor argument, not a call to {@code Instant.now()} in the middle of compiling,
     * so a caller that needs a reproducible statement — a test, a cached plan — can ask for one.</p>
     */
    public Instant now() {
        return now;
    }

    public Dialect dialect() {
        return dialect;
    }

    public QuerySchema schema() {
        return schema;
    }

    /**
     * What the document is about — the table, its alias and its key.
     *
     * <p>⚠️ A whole target rather than a bare alias. A mapping needs the key column to join against, and
     * one that assembled {@code alias + ".id"} by hand would be free to spell the alias differently from
     * the {@code FROM} clause — producing a join that compiles and matches nothing.</p>
     */
    public QueryTarget target() {
        return target;
    }

    /** What the rows being filtered are called in the statement being built. */
    public String rootAlias() {
        return target.alias();
    }

    /**
     * The target's {@code FROM} clause, quoted this dialect's way — {@code `entries` e}.
     *
     * <p>⚠️ Quoting lives here rather than on {@link QueryTarget} because only this holds the dialect. A
     * record rendering its own SQL would have to render it <em>unquoted</em>, and a statement that quotes
     * some identifiers and not others works right up until a table is called {@code order}.</p>
     */
    public String from() {
        return "%s %s".formatted(dialect.quote(target.table()), target.alias());
    }

    /**
     * The target's key, qualified and quoted — {@code e.`id`}.
     *
     * <p>Written once because every bag join needs it, and a mapping assembling it by hand would be free
     * to spell the alias differently from the {@code FROM} clause — producing a join that compiles and
     * matches nothing.</p>
     */
    public String rootKey() {
        return column(target.key());
    }

    /**
     * A column of the target's own table, qualified and quoted — {@code e.`created_at`}.
     *
     * @param column the column as the store spells it
     * @return the column, qualified and quoted
     */
    public String column(String column) {
        return "%s.%s".formatted(target.alias(), dialect.quote(column));
    }

    /**
     * A column of a joined table, qualified and quoted — {@code j1.`text_value`}.
     *
     * @param alias  the join's alias — generated, so never quoted
     * @param column the column as the store spells it
     * @return the column, qualified and quoted
     */
    public String column(String alias, String column) {
        return "%s.%s".formatted(alias, dialect.quote(column));
    }

    /**
     * The alias for this key, allocating one the first time and reusing it afterwards.
     *
     * <p>⚠️ Reuse is what makes naming an attribute twice cheap; one alias per <em>key</em> rather than
     * per <em>reference</em> is what keeps two different attributes from being asked to be one row.</p>
     *
     * @param key what the alias is for — normally the attribute's name
     * @return a stable alias
     */
    public String alias(String key) {
        return aliases.computeIfAbsent(key, ignored -> aliasPrefix + (aliases.size() + 1));
    }

    /**
     * Registers a join, once per alias.
     *
     * <p>Called by an {@link AttributeMapping} while answering what an attribute reads as. An attribute
     * named three times in one {@code where} asks for the same join three times, so a repeat is a no-op
     * rather than an error.</p>
     *
     * <p>⚠️ <strong>The join's own values belong in the fragment handed here</strong>, not bound through
     * anything else: they are written into the statement before the clause that discovered them, and a
     * value that travelled with the clause instead would bind in the wrong place.</p>
     *
     * @param alias the alias this join introduces
     * @param join  the join, with any values it binds
     */
    public void join(String alias, Fragment join) {
        if (joined.add(alias)) {
            joins.add(join);
        }
    }

    /** Every join registered, in registration order, as one fragment. */
    public Fragment joins() {
        Fragment assembled = Fragment.empty();

        for (Fragment join : joins) {
            assembled = assembled.then(join, " ");
        }

        return assembled;
    }
}
