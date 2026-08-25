package org.jmouse.query.translate.row;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.PropertyNode;
import org.jmouse.el.node.expression.ArgumentsNode;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.node.expression.LiteralNode;
import org.jmouse.query.translate.Capabilities;
import org.jmouse.query.translate.Capability;
import org.jmouse.query.translate.Bindings;
import org.jmouse.query.translate.DeclaredValues;
import org.jmouse.query.translate.Translator;
import org.jmouse.query.translate.UnsupportedQueryException;
import org.jmouse.query.el.QueryFunctions;
import org.jmouse.query.el.QueryLanguage;
import org.jmouse.query.el.function.Rewriter;
import org.jmouse.query.el.node.ColumnsNode;
import org.jmouse.query.el.node.LimitNode;
import org.jmouse.query.el.node.OrderNode;
import org.jmouse.query.el.node.QueryBlockNode;
import org.jmouse.query.el.node.WhereNode;
import org.jmouse.query.el.node.AttributeNode;
import org.jmouse.query.el.node.SourceNode;
import org.jmouse.query.schema.QueryAttribute;
import org.jmouse.query.schema.QuerySchema;
import org.jmouse.query.schema.QueryType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

/**
 * A second backend — the same query, run over a list of maps.
 *
 * <h2>⚠️ Why this exists, and it is not only for tests</h2>
 *
 * <p>It is the thing that proves the language is not SQL wearing a hat. A query written once and run
 * against both a database and a list of rows either means the same thing in both or it does not, and
 * until there were two backends nobody could tell. It is also how the language can be exercised with no
 * database at all — which is what a builder's preview, a validation pass and a unit test each need.</p>
 *
 * <h2>⚠️ It refuses more than the SQL adapter, on purpose</h2>
 *
 * <p>No {@code group}, no {@code join}. Both are honest: there is nothing to join a flat list to, and
 * aggregating in memory is real work that has not been done. Declaring them and returning ungrouped rows
 * would be the exact failure {@link Capabilities} exists to prevent.</p>
 *
 * <h2>⚠️ The clock is FOLDED, not evaluated</h2>
 *
 * <p>{@code now() - days(30)} is worked out once while the query is being rewritten and put where a
 * supplied value goes — the same shape the SQL side gives it, where the moment is one bound parameter
 * rather than a call the database makes. One instant per translation, so two clauses asking the clock
 * cannot see two answers, and the two backends cannot come to disagree about when "now" was.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class RowTranslator implements Translator<RowTranslator.Query> {

    private static final Capabilities CAPABILITIES = Capabilities.of("row",
            Capability.FILTER,
            Capability.SORT,
            Capability.PROJECT,
            Capability.CONVERT,
            Capability.CLOCK,
            Capability.LIMIT);

    /**
     * ⚠️ Which calendar unit each duration function means — the row side's twin of the SQL compiler's
     * interval table, and a closed map for the same reason: a unit is never something a query's text can
     * hand over.
     */
    private static final Map<String, ChronoUnit> UNITS = Map.of(
            "seconds", ChronoUnit.SECONDS,
            "minutes", ChronoUnit.MINUTES,
            "hours", ChronoUnit.HOURS,
            "days", ChronoUnit.DAYS,
            "weeks", ChronoUnit.WEEKS,
            "months", ChronoUnit.MONTHS,
            "years", ChronoUnit.YEARS);

    private final QuerySchema         schema;
    private final ExpressionLanguage  language;
    private final Map<String, Object> values;
    private final Map<String, String> columns;

    /**
     * The names that stand in for a set somewhere in this installation.
     *
     * <p>⚠️ Held only so the refusal says the right thing. Without it, {@code x in supportPeople} came back
     * as "there is nothing called 'supportPeople' here" — true, and the wrong answer to read: it sounds
     * like a typo when the real answer is that this destination cannot honour a subquery.</p>
     */
    private Set<String> subqueries = Set.of();

    /** Tells this translator which names are views, so it can refuse them for the right reason. */
    public void subqueries(Set<String> names) {
        this.subqueries = Set.copyOf(names);
    }

    /**
     * A translator over rows a MAPPING describes — a CSV file, a spreadsheet, anything whose columns are
     * not already named the way a query writes them.
     *
     * <p>⚠️ This is what makes "a file is not a backend" true rather than a slogan. A CSV and a list of
     * maps reach the same pipeline; the mapping is the only thing that differs, and it is the mapping that
     * turns a header called {@code "Request"} into the attribute {@code request.key}.</p>
     *
     * @param source a structure bound to a place — the merged declaration
     */
    public RowTranslator(SourceNode source) {
        this(source, new QueryLanguage().expressionLanguage(), Map.of());
    }

    public RowTranslator(SourceNode source, ExpressionLanguage language, Map<String, Object> values) {
        this.schema = schemaOf(source);
        this.language = language;
        this.values = Map.copyOf(values);
        this.columns = columnsOf(source);
    }

    public RowTranslator(QuerySchema schema) {
        this(schema, new QueryLanguage().expressionLanguage());
    }

    public RowTranslator(QuerySchema schema, ExpressionLanguage language) {
        this(schema, language, Map.of());
    }

    /**
     * ⚠️ The same values the SQL side binds — {@code currentMember}, {@code blockedIds} — and the same
     * names. Here they become variables in the evaluation context rather than parameters in a statement,
     * which is exactly the point: <strong>one query text, two mechanisms, one meaning.</strong>
     *
     * <p>A name that is neither an attribute nor a supplied value is refused, as it is by the checker.</p>
     *
     * @param values what the caller supplies by name
     */
    public RowTranslator(QuerySchema schema, ExpressionLanguage language, Map<String, Object> values) {
        this.schema = schema;
        this.language = language;
        this.values = Map.copyOf(values);
        this.columns = Map.of();
    }

    /**
     * The shape, read straight off the merged declaration.
     *
     * <p>⚠️ Built here rather than borrowed from the SQL module, which depends on this one and not the
     * other way round. A row backend needing a database on the classpath in order to read a file would be
     * the dependency this whole seam exists to avoid.</p>
     */
    private static QuerySchema schemaOf(SourceNode source) {
        Map<String, QueryAttribute> attributes = new LinkedHashMap<>();

        for (AttributeNode declared : source.getAttributes()) {
            attributes.put(declared.getName(), new QueryAttribute(
                    declared.getName(), declared.getSource(),
                    typeOf(declared.getType()), accessOf(declared.getAccess())));
        }

        return new QuerySchema() {

            @Override
            public Optional<QueryAttribute> attribute(String name) {
                return Optional.ofNullable(attributes.get(name));
            }

            @Override
            public Collection<QueryAttribute> attributes() {
                return attributes.values();
            }
        };
    }

    /**
     * Where each attribute is read from in a RAW row — {@code request.key} from a cell called
     * {@code "Request"}.
     *
     * <p>⚠️ Empty when the mapping is {@code identity}, and an empty map means "the row is already keyed
     * the way a query writes it". That is the ordinary case for a list somebody built in Java, and doing
     * it by absence rather than by an identity map keeps the common path free of a lookup per cell.</p>
     */
    private static Map<String, String> columnsOf(SourceNode source) {
        Map<String, String> columns = new LinkedHashMap<>();

        for (AttributeNode declared : source.getAttributes()) {
            if (!declared.getName().equals(declared.getSource())) {
                columns.put(declared.getName(), declared.getSource());
            }
        }

        return columns;
    }

    private static QueryType typeOf(String written) {
        return written == null ? QueryType.UNKNOWN : switch (written) {
            case "text", "string" -> QueryType.TEXT;
            case "number", "int" -> QueryType.NUMBER;
            case "boolean" -> QueryType.BOOLEAN;
            case "temporal" -> QueryType.TEMPORAL;
            default -> QueryType.UNKNOWN;
        };
    }

    private static QueryAttribute.Access accessOf(String written) {
        return written == null ? QueryAttribute.Access.COLUMN : switch (written) {
            case "bag" -> QueryAttribute.Access.BAG;
            case "join" -> QueryAttribute.Access.JOINED;
            case "collection" -> QueryAttribute.Access.COLLECTION;
            default -> QueryAttribute.Access.COLUMN;
        };
    }

    @Override
    public Capabilities capabilities() {
        return CAPABILITIES;
    }

    /**
     * Translates a whole block, one clause of one, or a bare condition.
     *
     * <h2>⚠️ A clause on its own is a whole query here, and that is not a shortcut</h2>
     *
     * <p>Handed one {@code where}, this produces a {@link Query} that filters and returns whole rows —
     * exactly what a product wants when it holds a filter somebody composed and a list it fetched some
     * other way. It is the same tree, walked by the same code; only the entry point differs.</p>
     *
     * @param node     a view, a function body, a {@code where}, or a bare condition
     * @param bindings what the caller supplies by name, over whatever this translator was built with
     * @return the compiled query
     */
    @Override
    public Query translate(Node node, Bindings bindings) {
        Map<String, Object> supplied = supplied(bindings);

        if (node instanceof QueryBlockNode block) {
            return block(block, supplied);
        }

        if (node instanceof WhereNode clause) {
            return condition(clause.getCondition(), supplied);
        }

        if (node instanceof Expression condition) {
            return condition(condition, supplied);
        }

        throw new UnsupportedQueryException(
                "the '%s' translator was handed a %s; it reads a block, a 'where' or a condition"
                        .formatted(CAPABILITIES.translator(), node.getClass().getSimpleName()));
    }

    private Query block(QueryBlockNode block, Map<String, Object> given) {
        requireSupport(block);

        // ⚠️ Same rule as the SQL side, in the same words: a declared name arrives with a value, or with
        // the expression that stands in for it, or the translation is refused. Two backends disagreeing
        // about whether a missing value is an error is two languages.
        DeclaredValues.Declared declared = DeclaredValues.resolve(block, Bindings.of(given));
        Map<String, Object>     supplied = declared.asMap();

        Names                        names   = new Names(supplied, declared.defaults());
        List<OrderNode.Key>          order   = new ArrayList<>();
        List<ColumnsNode.Projection> columns = new ArrayList<>();

        Expression where = block.getWhere()
                .map(clause -> names.rewrite(clause.getCondition()))
                .orElse(null);

        block.getOrder().ifPresent(clause -> clause.getKeys().forEach(key ->
                order.add(new OrderNode.Key(names.rewrite(key.expression()), key.direction()))));

        block.getColumns().ifPresent(clause -> clause.getProjections().forEach(projection ->
                columns.add(new ColumnsNode.Projection(names.rewrite(projection.expression()),
                        projection.alias()))));

        int limit = block.getClauses().stream()
                .filter(LimitNode.class::isInstance)
                .map(LimitNode.class::cast)
                .mapToInt(LimitNode::getCount)
                .findFirst()
                .orElse(0);

        return new Query(language, names, names.bound(supplied), where, order, columns, limit);
    }

    private Query condition(Expression condition, Map<String, Object> supplied) {
        Names      names   = new Names(supplied);
        Expression written = names.rewrite(condition);

        return new Query(language, names, names.bound(supplied), written, List.of(), List.of(), 0);
    }

    /**
     * ⚠️ The call's bindings sit ON TOP of whatever this translator was constructed with, so the
     * three-argument constructor still means what it always meant — values this translator always
     * supplies — and a caller can add to them without building a second translator.
     */
    private Map<String, Object> supplied(Bindings bindings) {
        if (bindings.isEmpty()) {
            return values;
        }

        Map<String, Object> merged = new LinkedHashMap<>(values);

        merged.putAll(bindings.asMap());

        return merged;
    }

    /**
     * Renames every attribute reference to a plain variable, and remembers which was which.
     *
     * <p>⚠️ <strong>Necessary because {@code entry[quantity]} is a path, not a name.</strong> Evaluating
     * it would send the engine looking for a map called {@code entry} — so each reference becomes
     * {@code v1}, {@code v2} … and the row's values are put in the context under those. The rewrite uses
     * the same {@link Rewriter} the function inliner does, so a node kind added later refuses here too
     * rather than silently vanishing from a condition.</p>
     */
    public final class Names extends Rewriter {

        private final Map<String, String>     variables = new LinkedHashMap<>();
        private final Map<String, Object>     supplied;
        private final Map<String, Expression> defaults;

        /**
         * The declared names whose defaults are being put in place right now.
         *
         * <p>⚠️ Held so that {@code v(a : b, b : a)} is refused by name rather than recursing until the
         * stack gives out — the same guard, in the same words, as the SQL compiler's.</p>
         */
        private final Set<String> standingIn = new LinkedHashSet<>();

        /**
         * ⚠️ ONE instant for this whole translation, taken when the rewriter is made.
         *
         * <p>Two clauses each asking the clock must agree, or a query translated across a second boundary
         * keeps rows satisfying neither of them. It is the same rule the SQL side follows by binding one
         * per statement, and it is the only reason one document can mean one thing on both.</p>
         */
        private final Instant now = Instant.now();

        /** Every moment worked out of the text, by the name that now stands for it. */
        private final Map<String, Object> folded = new LinkedHashMap<>();

        /** Where each attribute is read from in a raw row. Empty means the row is already keyed by name. */
        public Map<String, String> columns() {
            return columns;
        }

        private Names(Map<String, Object> supplied) {
            this(supplied, Map.of());
        }

        private Names(Map<String, Object> supplied, Map<String, Expression> defaults) {
            this.supplied = supplied;
            this.defaults = defaults;
        }

        @Override
        public Expression visitProperty(PropertyNode property) {
            // ⚠️ A supplied value passes through UNRENAMED: it is not read off a row, so it has no column
            // to be renamed to — it is put into the context under the name the query wrote.
            if (supplied.containsKey(property.getPath())) {
                return property;
            }

            // ⚠️ A declared name nobody supplied is REPLACED BY ITS DEFAULT'S TREE, not by a value worked
            // out beforehand — the same rule as the SQL side, so one document means one thing on both.
            if (defaults.containsKey(property.getPath())) {
                return standingIn(property.getPath());
            }

            if (subqueries.contains(property.getPath())) {
                CAPABILITIES.require(Capability.SUBQUERY, property.getPath() + " as a set");
            }

            schema.attribute(property.getPath()).orElseThrow(() -> new IllegalArgumentException(
                    "there is nothing called '%s' here".formatted(property.getPath())));

            return new PropertyNode(variables.computeIfAbsent(
                    property.getPath(), ignored -> "v" + (variables.size() + 1)));
        }

        /**
         * {@code now() - days(30)} — worked out here, once, and bound.
         *
         * <h2>⚠️ Folded while rewriting, never evaluated per row</h2>
         *
         * <p>The SQL side binds one instant per statement so that every clause of one query means the same
         * moment. This is that rule, in the same place it belongs on this side: the whole moment
         * expression collapses to a single value at translation time, and a query asking the clock twice
         * cannot see two answers.</p>
         *
         * <p>⚠️ It becomes a <strong>bound value</strong>, exactly as it becomes a bound parameter in SQL —
         * the same mechanism, not a parallel one. That is also what keeps the moment out of the expression
         * tree the evaluator walks per row.</p>
         */
        @Override
        public Expression visitBinary(BinaryOperation operation) {
            Token.Type type = operation.getOperator().getType();

            if ((type == BasicToken.T_PLUS || type == BasicToken.T_MINUS)
                && operation.getRight() instanceof FunctionNode call
                && QueryFunctions.isDuration(call.getName())) {

                Instant moment = momentOf(operation.getLeft());

                if (moment != null) {
                    return bind(shift(moment, call, type == BasicToken.T_MINUS));
                }
            }

            return super.visitBinary(operation);
        }

        /** A bare {@code now()}, with no duration applied to it. */
        @Override
        public Expression visitCall(FunctionNode call) {
            if (QueryFunctions.NOW.equals(call.getName())) {
                return bind(now);
            }

            // ⚠️ A duration reaching here is one that was NOT applied to a moment — `days(7)` alone, or
            // `request.hours - days(7)`. It is meaningless either way, and is refused rather than read as
            // the number seven, which is the same refusal the SQL compiler gives.
            if (QueryFunctions.isDuration(call.getName())) {
                throw new UnsupportedQueryException(
                        ("'%s(…)' is a length of time and means nothing on its own — add it to or "
                         + "subtract it from a moment, as in \"now() - days(7)\"")
                                .formatted(call.getName()));
            }

            return super.visitCall(call);
        }

        /**
         * The moment an expression is, or {@code null} where it is not one.
         *
         * <p>⚠️ Only {@code now()} and a moment already shifted — a row's own value is not a candidate,
         * because a duration applied to a column would have to be worked out per row and this whole
         * mechanism exists to work the moment out once.</p>
         */
        private Instant momentOf(Expression expression) {
            if (expression instanceof FunctionNode call && QueryFunctions.NOW.equals(call.getName())) {
                return now;
            }

            if (expression instanceof BinaryOperation shifted
                && shifted.getRight() instanceof FunctionNode call
                && QueryFunctions.isDuration(call.getName())) {

                Instant inner = momentOf(shifted.getLeft());

                return inner == null
                        ? null
                        : shift(inner, call, shifted.getOperator().getType() == BasicToken.T_MINUS);
            }

            return null;
        }

        /**
         * ⚠️ Through a calendar, not by adding seconds. A month is not thirty days and a year is not three
         * hundred and sixty-five of them; {@code Instant} itself refuses those units for exactly that
         * reason. The zone is the machine's, which is the only one a list of maps carries any information
         * about — a database answers the same question in its own, and a query that cares about the
         * difference should take the moment as a supplied value rather than ask either clock.
         */
        private Instant shift(Instant moment, FunctionNode duration, boolean subtract) {
            long        amount = amountOf(duration);
            ChronoUnit  unit   = UNITS.get(duration.getName());
            ZonedDateTime local = moment.atZone(ZoneId.systemDefault());

            return (subtract ? local.minus(amount, unit) : local.plus(amount, unit)).toInstant();
        }

        private long amountOf(FunctionNode duration) {
            Expression written = duration.getArguments();

            if (written instanceof ArgumentsNode arguments && !arguments.getChildren().isEmpty()
                && arguments.getChildren().getFirst() instanceof Expression first) {
                written = first;
            }

            if (written instanceof LiteralNode<?> literal && literal.getValue() instanceof Number amount) {
                return amount.longValue();
            }

            // ⚠️ A supplied name counts, exactly as it does on the SQL side. `days(within)` inside a
            // function whose caller passes the count is the ordinary shape, and a backend that took it
            // while the other refused it would be the divergence this class exists to disprove.
            if (written instanceof PropertyNode property && supplied.get(property.getPath()) instanceof Number amount) {
                return amount.longValue();
            }

            throw new UnsupportedQueryException(
                    ("'%s(…)' needs a whole number of them — '%s' is not one")
                            .formatted(duration.getName(),
                                    written == null ? "nothing" : written.toSource()));
        }

        /** Puts a worked-out moment where a supplied value goes, and writes the name that reads it. */
        private Expression bind(Instant moment) {
            String name = "%s$%d".formatted(QueryFunctions.NOW, folded.size() + 1);

            folded.put(name, moment);

            return new PropertyNode(name);
        }

        /** What the caller supplied, plus every moment folded out of the text. */
        Map<String, Object> bound(Map<String, Object> supplied) {
            if (folded.isEmpty()) {
                return supplied;
            }

            Map<String, Object> everything = new LinkedHashMap<>(supplied);

            everything.putAll(folded);

            return everything;
        }

        /** The default's tree, rewritten in place of the name that stands on it. */
        private Expression standingIn(String name) {
            if (!standingIn.add(name)) {
                throw new UnsupportedQueryException(
                        ("'%s' stands on a default that leads back to itself; the names involved are %s")
                                .formatted(name, String.join(" → ", standingIn) + " → " + name));
            }

            try {
                return rewrite(defaults.get(name));
            } finally {
                standingIn.remove(name);
            }
        }

        Map<String, String> variables() {
            return variables;
        }
    }

    /** A compiled in-memory query — a filter, a sort and a projection over a list of maps. */
    public record Query(ExpressionLanguage language, Names names, Map<String, Object> values,
                        Expression where, List<OrderNode.Key> order,
                        List<ColumnsNode.Projection> columns, int limit) {

        /**
         * ⚠️ The eight-argument shape kept meaning what it meant — no limit.
         *
         * <p>A record gaining a component changes its canonical constructor, and every caller with it.
         * Keeping the old arity as a delegate is what stops growing the language from making somebody
         * re-read code they already wrote.</p>
         */
        public Query(ExpressionLanguage language, Names names, Map<String, Object> values,
                     Expression where, List<OrderNode.Key> order,
                     List<ColumnsNode.Projection> columns) {
            this(language, names, values, where, order, columns, 0);
        }

        /**
         * Runs it.
         *
         * @param rows the rows, each keyed by the name a query writes — {@code entry[quantity]}
         * @return what the query asked for
         */
        public List<Map<String, Object>> run(List<Map<String, Object>> rows) {
            List<Map<String, Object>> kept = new ArrayList<>();

            for (Map<String, Object> row : rows) {
                if (where == null || Boolean.TRUE.equals(evaluate(where, row, Boolean.class))) {
                    kept.add(row);
                }
            }

            for (int index = order.size() - 1; index >= 0; index--) {
                OrderNode.Key key = order.get(index);
                Comparator<Map<String, Object>> comparator = Comparator.comparing(
                        row -> evaluate(key.expression(), row, Comparable.class),
                        Comparator.nullsLast(Comparator.naturalOrder()));

                kept.sort(key.direction() == OrderNode.Direction.DESCENDING
                        ? comparator.reversed()
                        : comparator);
            }

            // ⚠️ After the sort and before the projection — the same place SQL puts it. Limiting first
            // would return a different set of rows, and limiting after projecting would count tuples.
            if (limit > 0 && kept.size() > limit) {
                kept = new ArrayList<>(kept.subList(0, limit));
            }

            if (columns.isEmpty()) {
                return kept;
            }

            List<Map<String, Object>> projected = new ArrayList<>();

            for (Map<String, Object> row : kept) {
                Map<String, Object> tuple = new LinkedHashMap<>();
                int position = 0;

                for (ColumnsNode.Projection projection : columns) {
                    position++;

                    String label = projection.alias() != null
                            ? projection.alias()
                            : "column" + position;

                    tuple.put(label, evaluate(projection.expression(), row, Object.class));
                }

                projected.add(tuple);
            }

            return projected;
        }

        @SuppressWarnings("unchecked")
        private <T> T evaluate(Expression expression, Map<String, Object> row, Class<T> type) {
            EvaluationContext context = language.newContext();

            // ⚠️ Read by the name the ROW uses, which is the mapping's job to know. For a list already
            // keyed the way a query writes it the two are the same string and this costs one lookup;
            // for a file it is what turns a cell called "Request" into `request.key`.
            names.variables().forEach((path, variable) ->
                    context.setValue(variable, row.get(names.columns().getOrDefault(path, path))));

            // ⚠️ Set AFTER the row, and they cannot collide: a name that is a supplied value was never
            // renamed to a `v…` variable, and one that is both is refused before anything is compiled.
            values.forEach(context::setValue);

            Object value = expression.evaluate(context);

            return value == null ? null : (T) value;
        }
    }
}
