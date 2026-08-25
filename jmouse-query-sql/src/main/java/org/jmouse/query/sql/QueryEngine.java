package org.jmouse.query.sql;

import org.jmouse.el.node.Expression;
import org.jmouse.jdbc.dialect.Dialect;
import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.el.QueryFunctions;
import org.jmouse.query.el.function.FunctionInliner;
import org.jmouse.query.el.node.QueryBlockNode;
import org.jmouse.query.el.node.QueryDocumentNode;
import org.jmouse.query.el.node.ClauseNode;
import org.jmouse.query.el.node.JoinClauseNode;
import org.jmouse.query.el.node.ViewNode;
import org.jmouse.query.schema.QueryChecker;
import org.jmouse.query.schema.QuerySchema;

import org.jmouse.query.translate.Bindings;
import org.jmouse.query.translate.SourceBinding;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * jMQ, configured once for a product — the way a product actually holds it.
 *
 * <pre>{@code
 * QueryEngine engine = QueryEngine.with(new MySqlDialect())
 *         .source("inventory", entriesTarget, entriesSchema, bagMapping)
 *         .source("issues",    issuesTarget,  issuesSchema,  ColumnMapping.qualified())
 *         .build();
 *
 * Fragment statement = engine.compileDocument(savedView);            // a stored view
 * Fragment statement = engine.compileFilter("inventory", urlFilter); // an ad-hoc filter
 * }</pre>
 *
 * <h2>⚠️ Why this exists: the checker was optional, and it must not be</h2>
 *
 * <p>Running a query used to mean assembling seven things by hand and getting the order right. The
 * dangerous part was not the tedium — it was that {@link QueryChecker} was a <em>step a caller could
 * forget</em>. Skip it and an untyped comparison compiles straight through: {@code "900" > "1000"} is
 * true as text, so the query answers wrongly on every row with nothing anywhere to say so.</p>
 *
 * <p>Here there is no method that compiles without checking first. Not as a convention — as the only
 * available shape.</p>
 *
 * <h2>⚠️ It is a facade, not a wall</h2>
 *
 * <p>Everything underneath stays reachable and usable on its own. {@link #compile(ViewNode)} hands back
 * the {@link ViewCompiler.CompiledQuery} <em>parts</em> rather than a finished statement, so a caller
 * can add its own {@code SELECT}, compose a tenant condition onto the {@code WHERE}, or count instead of
 * fetching. {@link QuerySource} can be built and used without registering anything. A product that
 * outgrows this class should be able to stop using it without rewriting anything below it — if it ever
 * cannot, the facade has become a gate and that is a defect.</p>
 *
 * <h2>⚠️ The dialect is held, not baked in</h2>
 *
 * <p>{@link #forDialect} returns the same configuration pointed at another database. One product
 * supporting MySQL and PostgreSQL configures its sources once and keeps two engines — rather than
 * threading a dialect through every call, which is how one path ends up on the wrong one.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryEngine {

    private final QueryLanguage            language;
    private final Dialect                  dialect;
    private final Map<String, QuerySource> sources;
    private final Targets                  targets;
    private final Map<String, ViewNode>    views;

    private QueryEngine(QueryLanguage language, Dialect dialect, Map<String, QuerySource> sources,
                        Targets targets, Map<String, ViewNode> views) {
        this.language = language;
        this.dialect = dialect;
        this.sources = Map.copyOf(sources);
        this.targets = targets;
        this.views = Map.copyOf(views);
    }

    public static Builder with(Dialect dialect) {
        return new Builder(dialect);
    }

    /** The same sources, pointed at another database. */
    public QueryEngine forDialect(Dialect other) {
        return new QueryEngine(language, other, sources, targets, views);
    }

    /** The language itself, for a caller that wants to parse or rewrite without compiling. */
    public QueryLanguage language() {
        return language;
    }

    public Dialect dialect() {
        return dialect;
    }

    /**
     * A registered source, by the name a document writes after {@code on}.
     *
     * @param name the target's name
     * @return the source, or empty when nothing is registered under that name
     */
    public Optional<QuerySource> source(String name) {
        return Optional.ofNullable(sources.get(name));
    }

    /**
     * Compiles a stored {@code .jmq} document holding exactly one view.
     *
     * @param source the document's text
     * @return a runnable statement and its values
     */
    public Fragment compileDocument(String source) {
        return compileDocument(source, Map.of());
    }

    /**
     * Compiles a stored {@code .jmq} document against values the caller supplies.
     *
     * <h2>⚠️ A value is the CALLER's word, and a query can never invent one</h2>
     *
     * <p>{@code issue.assignee == currentMember} is the shape every product's saved filters take, and the
     * tempting implementation is to paste the value into the text before parsing. That is a concatenated
     * query — a display name with an apostrophe breaks it, and a chosen one rewrites it. Here the name
     * survives as a name and becomes a bound {@code ?} at the very end.</p>
     *
     * <p>⚠️ It is also what keeps a saved view honest: {@code currentMember} means whoever is running it,
     * because the row cannot carry an answer to that question. A stored scope is a stored answer, and
     * that is how a view saved by one person shows another person's data.</p>
     *
     * @param source the document's text
     * @param values what the caller supplies by name
     * @return a runnable statement and its values
     */
    public Fragment compileDocument(String source, Map<String, Object> values) {
        QueryDocumentNode document = language.document(source);
        ViewNode view = singleView(document);

        // ⚠️ Calls are inlined BEFORE the schema is checked. A function body names its parameters, and a
        // checker asked about `threshold` would refuse it as an attribute nothing declares. Inlining
        // first means everything downstream sees one ordinary condition — the checker, the compiler and
        // any future backend need to know nothing about functions at all.
        new FunctionInliner(document, QueryFunctions.BUILT_IN).inline(view);

        return compile(view, values).select();
    }

    /**
     * Compiles a bare filter written against a named source — what arrives in a URL, a header, a config
     * value or an agent's tool call.
     *
     * <p>⚠️ Checked exactly as a document is. An expression from a URL is the <em>least</em> trusted
     * input this language takes, so it is the last place a check should be skippable.</p>
     *
     * @param targetName which source it is about
     * @param filter     the expression
     * @return a runnable statement and its values
     */
    public Fragment compileFilter(String targetName, String filter) {
        return compileFilter(targetName, filter, Map.of());
    }

    /**
     * Compiles a bare filter against values the caller supplies.
     *
     * @param targetName which source it is about
     * @param filter     the expression
     * @param values     what the caller supplies by name — {@code currentMember}, a tenant
     * @return a runnable statement and its values
     */
    public Fragment compileFilter(String targetName, String filter, Map<String, Object> values) {
        return compileCondition(targetName, filter, values).select();
    }

    /**
     * A bare filter compiled into its <strong>parts</strong>, for a caller that owns the projection.
     *
     * <p>⚠️ Offered because the alternative is what a caller actually did: take the finished
     * {@code SELECT *} and rewrite its projection with a regular expression. That works until a value
     * bound in the projection has to come first — which is exactly the ordering
     * {@link ViewCompiler.CompiledQuery#select(Fragment)} exists to get right — and it puts a parser for
     * this compiler's own output in somebody else's module.</p>
     *
     * @param targetName which source it is about
     * @param filter     the expression
     * @param values     what the caller supplies by name
     * @return the parts, with an empty projection
     */
    public ViewCompiler.CompiledQuery compileCondition(
            String targetName, String filter, Map<String, Object> values) {

        QuerySource source = require(targetName);
        Expression  parsed = language.expression(filter);

        // ⚠️ checkCondition, not check: this text chooses ROWS, so it is refused an aggregate exactly as
        // a `where` clause is. The two used to differ, and the entry point with the weaker check was the
        // one reading a URL.
        new QueryChecker(source.schema(), values.keySet()).checkCondition(parsed);

        SqlContext  context  = context(source, values);
        SqlCompiler compiler = new SqlCompiler(source.mapping(), source.membership(), context);
        Fragment    where    = compiler.compile(parsed);

        return new ViewCompiler.CompiledQuery(
                context.from(), Fragment.empty(), context.joins(), where,
                Fragment.empty(), Fragment.empty(), Fragment.empty(), false);
    }

    /**
     * One source, as a {@link org.jmouse.query.translate.Translator} — for a caller that wants to be
     * backend-agnostic rather than to hold SQL.
     *
     * <p>⚠️ The same registered source, reached through the interface every backend implements. A
     * builder's preview can then run a document in memory and the identical document against the
     * database without knowing which of the two it is talking to — and a clause a backend cannot honour
     * is refused by name rather than quietly dropped.</p>
     *
     * @param targetName which source it is about
     * @return a translator over it
     */
    public SqlTranslator translator(String targetName) {
        return new SqlTranslator(require(targetName), dialect);
    }

    /**
     * Compiles a parsed view into its parts, so a caller can assemble the statement itself.
     *
     * <p>⚠️ The parts, not a finished statement — this is the seam that keeps the facade from becoming a
     * wall. Row-level scoping composes a condition onto {@link ViewCompiler.CompiledQuery#where()}; a
     * count replaces the projection; a screen may want its own columns whatever the document said.</p>
     *
     * @param view a parsed view
     * @return the compiled parts
     */
    public ViewCompiler.CompiledQuery compile(ViewNode view) {
        return compile(view, Map.of());
    }

    /**
     * Compiles a parsed view against values the caller supplies.
     *
     * @param view   a parsed view
     * @param values what the caller supplies by name
     * @return the compiled parts
     */
    public ViewCompiler.CompiledQuery compile(ViewNode view, Map<String, Object> values) {
        // ⚠️ Through the translator, so the capability check cannot be skipped on this path. It was, and
        // the result was a `limit` clause that parsed, checked, compiled — and silently did not appear in
        // the statement. A clause quietly dropped is the one failure this whole design exists to prevent,
        // and the way it happened was a second route to the compiler that did not ask.
        return translator(view, Bindings.of(values)).parts(view, values);
    }

    /**
     * The translator for one view — resolving {@code on $source} against what the caller supplies.
     *
     * <h2>⚠️ Late binding is resolved HERE, because this is what holds the registry</h2>
     *
     * <p>A {@link SqlTranslator} is deliberately over one source; that is what makes it a translator for a
     * destination rather than a second registry. So the question <em>which source</em> is answered by the
     * thing that knows what has been declared, and the translator is handed the answer.</p>
     *
     * @param view     a parsed view, pinned or late-bound
     * @param bindings what the caller supplies by name
     * @return the translator for whatever it turned out to be about
     */
    public SqlTranslator translator(ViewNode view, Bindings bindings) {
        String      name  = SourceBinding.resolve(view, bindings, sources.keySet());
        QuerySource about = require(name);

        SqlTranslator translator =
                new SqlTranslator(joined(view, name, about), dialect, this::subquery, views.keySet());

        translator.language(language.expressionLanguage());

        return translator;
    }

    /**
     * The statement a named view stands for, where the name is one.
     *
     * <h2>⚠️ Compiled on demand, and refused when it projects more than one attribute</h2>
     *
     * <p>{ x in supportPeople} asks whether a value is one of a SET, so the inner view has to produce
     * exactly one column. Two is refused by name rather than the language picking the first — picking one
     * would answer a different question and look like it worked.</p>
     */
    private Optional<Fragment> subquery(String name) {
        return subquery(name, new LinkedHashSet<>());
    }

    /**
     * ⚠️ {@code being} is what stops a view standing in for itself.
     *
     * <p>{@code view 'x':a { where: k in a }} would otherwise compile {@code a} in order to compile
     * {@code a}, forever — a stack overflow rather than a message, and one a person writing a query can
     * reach by accident the moment two views reference each other.</p>
     */
    private Optional<Fragment> subquery(String name, Set<String> being) {
        ViewNode inner = views.get(name);

        if (inner == null) {
            return Optional.empty();
        }

        if (!being.add(name)) {
            throw new SqlCompileException(
                    ("'%s' stands in for a set that includes itself; the views involved are %s")
                            .formatted(name, String.join(" → ", being) + " → " + name));
        }

        int projected = inner.getColumns().map(columns -> columns.getProjections().size()).orElse(0);

        if (projected != 1) {
            throw new SqlCompileException(
                    ("'%s' stands in for a set, so it has to fetch exactly one attribute; it fetches %s")
                            .formatted(name, projected == 0 ? "none" : projected + " of them"));
        }

        try {
            SqlTranslator translator = new SqlTranslator(
                    require(innerSource(name, inner)), dialect,
                    deeper -> subquery(deeper, being), views.keySet());

            translator.language(language.expressionLanguage());

            return Optional.of(translator.parts(inner, Map.of()).select());
        } finally {
            being.remove(name);
        }
    }

    /**
     * Which source the inner view is about — and ⚠️ a refusal that says it is an INNER one.
     *
     * <p>A view standing in for a set is compiled where nobody passed it anything, so a late-bound one has
     * nothing to resolve against. Left alone the message reads {@code this view runs against '$x' and
     * nothing was bound to it} — true, and it names neither the view nor the fact that somebody else's
     * query is what pulled it in. Somebody would have to trace it back by hand.</p>
     */
    private String innerSource(String name, ViewNode inner) {
        try {
            return SourceBinding.resolve(inner, Bindings.none(), sources.keySet());
        } catch (RuntimeException unresolved) {
            throw new SqlCompileException(
                    ("'%s' stands in for a set here, and it is late-bound, so there is nothing to resolve it "
                     + "against: %s. Pin it with 'from: <source>', or do not use it as a set")
                            .formatted(name, unresolved.getMessage()));
        }
    }

    /** Every view this engine knows by name — what may stand in for a set. */
    public Set<String> declaredViews() {
        return views.keySet();
    }

    /**
     * The source a view actually compiles against — its own, or its own composed with everything it joins.
     *
     * <h2>⚠️ Co-location is checked here, before anything is compiled</h2>
     *
     * <p>Two structures in different places cannot appear in one statement, and the refusal names both and
     * says where each is. Deciding it at compile time rather than at run time is what makes it a message
     * instead of a database error nobody can read.</p>
     */
    private QuerySource joined(ViewNode view, String name, QuerySource about) {
        QuerySource composed = about;

        for (JoinClauseNode join : view.getClauses(JoinClauseNode.class)) {
            targets.requireTogether(name, join.getStructure());

            composed = JoinedStructures.compose(composed, require(join.getStructure()), join);
        }

        return composed;
    }

    /** Which mappings live in the same place — what a join asks about. */
    public Targets targets() {
        return targets;
    }

    /** The names anything late-bound may resolve to — what this engine was told about. */
    public Set<String> declared() {
        return Set.copyOf(sources.keySet());
    }

    private String target(ViewNode view, Map<String, Object> values) {
        return SourceBinding.resolve(view, Bindings.of(values), sources.keySet());
    }

    /**
     * Compiles a block against a source given directly, registering nothing.
     *
     * <p>For a one-off report, a test, or a screen with its own narrowed schema.</p>
     *
     * @param block  a view or a function body
     * @param source what it is about
     * @return the compiled parts
     */
    public ViewCompiler.CompiledQuery compile(QueryBlockNode block, QuerySource source) {
        return compile(block, source, Map.of());
    }

    /**
     * Compiles a block against a source given directly, with values the caller supplies.
     *
     * @param block  a view or a function body
     * @param source what it is about
     * @param values what the caller supplies by name
     * @return the compiled parts
     */
    public ViewCompiler.CompiledQuery compile(
            QueryBlockNode block, QuerySource source, Map<String, Object> values) {

        check(block, source.schema(), values.keySet());

        return new ViewCompiler(source.mapping(), source.membership())
                .compile(block, dialect, source.schema(), source.target(), values);
    }

    /**
     * ⚠️ A view's own declarations count as supplied names.
     *
     * <p>{@code view 'x':hot(since as temporal) uses(prefix as text)} may mention {@code since} and
     * {@code prefix}; the checker would otherwise refuse them as attributes nothing declares. Declaring
     * them is what makes the refusal of an UNdeclared one meaningful — without the list, every free name
     * would have to be allowed.</p>
     */
    private void check(QueryBlockNode block, QuerySchema schema, Set<String> values) {
        new QueryChecker(schema, allowedNames(block, values)).check(block);
    }

    static Set<String> allowedNames(QueryBlockNode block, Set<String> values) {
        return allowedNames(block, values, Set.of());
    }

    /**
     * ⚠️ A declared VIEW's name is legal in a query too, because a view may stand in for a set.
     *
     * <p>It is allowed only for names this engine actually holds, so a mistyped one is still refused as an
     * attribute nothing declares — which is what stops {@code x in supportPeaple} compiling into a bound
     * null and returning no rows.</p>
     */
    static Set<String> allowedNames(QueryBlockNode block, Set<String> values, Set<String> views) {
        Set<String> declared = block instanceof ViewNode view ? view.declaredNames() : Set.of();

        if (declared.isEmpty() && views.isEmpty()) {
            return values;
        }

        Set<String> allowed = new LinkedHashSet<>(values);

        allowed.addAll(declared);
        allowed.addAll(views);

        return allowed;
    }

    private SqlContext context(QuerySource source, Map<String, Object> values) {
        return new SqlContext(dialect, source.schema(), source.target(), SqlContext.DEFAULT_ALIAS_PREFIX,
                Instant.now(), values);
    }

    private QuerySource require(String name) {
        return source(name).orElseThrow(() -> new SqlCompileException(
                sources.isEmpty()
                        ? "nothing is registered to query; this engine has no sources"
                        : "there is nothing called '%s' to query; this engine has %s".formatted(
                                name, String.join(", ", sources.keySet()))));
    }

    private ViewNode singleView(QueryDocumentNode document) {
        return document.getSingleView().orElseThrow(() -> new SqlCompileException(
                document.getViews().isEmpty()
                        ? "this document declares no view to run"
                        : "this document declares %d views; name the one to run".formatted(
                                document.getViews().size())));
    }

    /** Collects the sources a product queries. */
    public static final class Builder {

        private final Dialect                  dialect;
        private final Map<String, QuerySource> sources = new LinkedHashMap<>();
        private final Map<String, ViewNode>    views   = new LinkedHashMap<>();
        private final Targets.Builder          targets = Targets.builder();

        private QueryLanguage language = new QueryLanguage();

        private Builder(Dialect dialect) {
            this.dialect = dialect;
        }

        /**
         * ⚠️ Offered so a product can supply a language carrying its own contributed functions —
         * {@code now()}, {@code currentUser()} — without this class knowing such things exist.
         */
        public Builder language(QueryLanguage language) {
            this.language = language;

            return this;
        }

        public Builder source(QuerySource source) {
            return source(Targets.DEFAULT, source);
        }

        /**
         * A source, in a named place.
         *
         * <p>⚠️ Only worth naming when a product genuinely has more than one — a second database, an
         * export nobody joins to. Everything registered without one shares {@link Targets#DEFAULT}, so a
         * product with a single database behaves exactly as it did and every structure is joinable.</p>
         */
        public Builder source(String target, QuerySource source) {
            sources.put(source.name(), source);
            targets.mapping(target, source.name());

            return this;
        }

        public Builder source(String name, QueryTarget target, QuerySchema schema, AttributeMapping mapping) {
            return source(new QuerySource(target, schema, mapping));
        }

        /**
         * Every {@code source { }} a {@code .jmq} document declares.
         *
         * <p>⚠️ <strong>The same registry as a source built in Java</strong>, on purpose. A product may
         * declare some of its sources in a file and others in code — a screen with a narrowed schema, a
         * source whose mapping genuinely needs logic — and neither the engine nor anything below it can
         * tell which is which. A declarative layer that produced a <em>different kind of thing</em> would
         * have been a second mechanism rather than a second spelling.</p>
         *
         * @param document a parsed document, usually a product's {@code sources.jmq}
         */
        public Builder sources(QueryDocumentNode document) {
            SourceLoader.load(document).forEach(this::source);

            // ⚠️ The views come too, and only so that one can stand in for a SET — `x in supportPeople`.
            // A view is registered under its IDENTIFIER, which is what another declaration writes down; a
            // title is shown, translated and reworded, and referencing one would break the day somebody
            // improved the wording.
            document.getViews().forEach(view ->
                    view.getIdentifier().ifPresent(name -> views.put(name, view)));

            return this;
        }

        /**
         * Reads a {@code .jmq} document's sources from text.
         *
         * @param source the document's text
         */
        public Builder sources(String source) {
            return sources(language.document(source));
        }

        public QueryEngine build() {
            return new QueryEngine(language, dialect, sources, targets.build(), views);
        }
    }
}
