package org.jmouse.query.spring;

import org.jmouse.query.sql.Fragment;
import org.jmouse.query.sql.QueryEngine;
import org.jmouse.query.sql.SqlCompileException;
import org.jmouse.query.sql.ViewCompiler;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns an interface of {@link JmQuery}-annotated methods into something that runs them.
 *
 * <pre>{@code
 * DeliveryQueries queries = repositories.create(DeliveryQueries.class);
 * List<Delivery> heavy = queries.heavyFor("meest", 100);
 * }</pre>
 *
 * <h2>⚠️ Every method is compiled when the repository is created</h2>
 *
 * <p>Not lazily, and not per call. A crooked query then fails the boot with the method's own name in the
 * message, which is the only moment anybody can still fix it; compiled on first use, the same mistake is
 * a 500 for whoever happens to be first. It is also why {@link #create} is deliberately eager and
 * slightly slow: this work is done once, at a moment when being slow is free.</p>
 *
 * <h2>⚠️ What it deliberately is not</h2>
 *
 * <p>Not Spring Data. There is no derived-query parsing, no {@code Pageable}, no entity manager and no
 * transaction of its own — a repository here is a compiled statement and a row mapper. Whether jMQ ever
 * backs a {@code Pageable} is a separate question, and asking it early would have made this module the
 * place where a query language grew an ORM.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryRepositories {

    /** What a bare condition is allowed to start with, and what is stripped before compiling it. */
    private static final String WHERE = "where ";

    /** The annotation's default projection — every column of the source's own table. */
    private static final String ANY_COLUMN = "*";

    private final QueryEngine engine;
    private final DataSource  dataSource;

    public QueryRepositories(QueryEngine engine, DataSource dataSource) {
        this.engine = engine;
        this.dataSource = dataSource;
    }

    /**
     * A running implementation of an interface of queries.
     *
     * @param contract the interface to implement
     * @param <T>      its type
     * @return a proxy that runs each method's query
     * @throws IllegalStateException when any method's query does not compile against its source
     */
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> contract) {
        Map<Method, Compiled> compiled = new LinkedHashMap<>();

        for (Method method : contract.getMethods()) {
            JmQuery declared = method.getAnnotation(JmQuery.class);

            if (declared != null) {
                compiled.put(method, compile(contract, method, declared));
            }
        }

        return (T) Proxy.newProxyInstance(
                contract.getClassLoader(), new Class<?>[]{contract}, new Repository(compiled));
    }

    /**
     * Parses and checks one method's query, at creation time.
     *
     * <p>⚠️ Checked with the method's parameter names declared as <strong>values</strong>, so
     * {@code carrier} is neither an attribute nor a typo — and a query naming something the signature
     * does not supply is refused here rather than binding null at run time.</p>
     */
    private Compiled compile(Class<?> contract, Method method, JmQuery declared) {
        List<String> names = new ArrayList<>();

        for (java.lang.reflect.Parameter parameter : method.getParameters()) {
            Parameter named = parameter.getAnnotation(Parameter.class);

            if (named == null) {
                throw new IllegalStateException(
                        ("%s.%s takes an argument the query cannot name; annotate it with "
                         + "@Parameter(\"…\")").formatted(contract.getSimpleName(), method.getName()));
            }

            names.add(named.value());
        }

        Map<String, Object> placeholders = new LinkedHashMap<>();

        // ⚠️ A sentinel rather than null, because a null value is refused by the compiler — rightly, since
        // `= ?` with nothing is never true in SQL. What is being checked here is the SHAPE of the query,
        // and any value of the right kind proves it.
        names.forEach(name -> placeholders.put(name, "<checked at startup>"));

        try {
            // ⚠️ Compiled with placeholder values rather than merely parsed: this is the pass that refuses
            // a misspelled attribute, an untyped comparison and an aggregate in a `where`. Parsing alone
            // would accept all three and leave them to the database.
            run(declared, placeholders);
        } catch (RuntimeException | Error refusal) {
            throw new IllegalStateException(
                    "%s.%s does not compile: %s".formatted(
                            contract.getSimpleName(), method.getName(), refusal.getMessage()), refusal);
        }

        return new Compiled(declared, List.copyOf(names), method.getReturnType(), rowType(method));
    }

    /** Compiles the query afresh for one call, with the caller's own values bound into it. */
    private Fragment run(JmQuery declared, Map<String, Object> values) {
        String text = declared.value().strip();

        if (engine.language().isDocument(text)) {
            return engine.compileDocument(text, values);
        }

        if (declared.source().isBlank()) {
            throw new SqlCompileException(
                    "this query is a bare condition, so it has to say which source it is about: "
                    + "@JmQuery(source = \"…\")");
        }

        String condition = text.startsWith(WHERE) ? text.substring(WHERE.length()) : text;

        // ⚠️ The PARTS, not the finished statement, so the projection is chosen rather than patched. The
        // first version took `SELECT *` back from the engine and rewrote it with a regular expression —
        // which put a parser for the compiler's output in this module, and would have bound a value in
        // the projection after the ones in the WHERE rather than before them.
        ViewCompiler.CompiledQuery parts = engine.compileCondition(declared.source(), condition, values);

        return ANY_COLUMN.equals(declared.select())
                ? parts.select()
                : parts.select(Fragment.of(declared.select()));
    }

    /** What one row becomes — a record's component type, a map, or a scalar. */
    private Class<?> rowType(Method method) {
        if (method.getGenericReturnType() instanceof ParameterizedType parameterized
            && parameterized.getActualTypeArguments().length == 1
            && parameterized.getActualTypeArguments()[0] instanceof Class<?> item) {

            return item;
        }

        return method.getReturnType();
    }

    /** One compiled method. */
    private record Compiled(JmQuery declared, List<String> names, Class<?> returnType, Class<?> rowType) {
    }

    /** The proxy: bind the arguments by name, compile, run, map. */
    private final class Repository implements InvocationHandler {

        private final Map<Method, Compiled> methods;

        private Repository(Map<Method, Compiled> methods) {
            this.methods = methods;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
            Compiled query = methods.get(method);

            if (query == null) {
                // toString, hashCode, equals and anything else the interface did not annotate.
                return switch (method.getName()) {
                    case "toString" -> "jMQ repository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (arguments == null ? null : arguments[0]);
                    default -> throw new UnsupportedOperationException(
                            "%s carries no @JmQuery".formatted(method.getName()));
                };
            }

            Map<String, Object> values = new LinkedHashMap<>();

            for (int index = 0; index < query.names().size(); index++) {
                values.put(query.names().get(index), arguments[index]);
            }

            return answer(query, rows(run(query.declared(), values)));
        }

        private Object answer(Compiled query, List<Map<String, Object>> rows) {
            Class<?> returned = query.returnType();

            if (returned == long.class || returned == Long.class
                || returned == int.class || returned == Integer.class) {

                long count = rows.isEmpty() ? 0 : asLong(rows.getFirst().values().iterator().next());

                return returned == int.class || returned == Integer.class ? (int) count : count;
            }

            if (returned == boolean.class || returned == Boolean.class) {
                return !rows.isEmpty();
            }

            if (returned == Optional.class) {
                return rows.isEmpty() ? Optional.empty() : Optional.of(map(rows.getFirst(), query.rowType()));
            }

            if (List.class.isAssignableFrom(returned)) {
                return rows.stream().map(row -> map(row, query.rowType())).toList();
            }

            return rows.isEmpty() ? null : map(rows.getFirst(), query.rowType());
        }
    }

    private static long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    /**
     * One row, as whatever the method asked for.
     *
     * <p>⚠️ A <strong>record</strong> is filled by component name, matched against the column label with
     * underscores removed and case ignored — so {@code full_name} lands on {@code fullName} and an alias
     * a query wrote wins over the column it came from. Anything else comes back as the map, which is
     * what a screen and a report both want.</p>
     */
    private static Object map(Map<String, Object> row, Class<?> type) {
        if (type == null || !type.isRecord()) {
            return row;
        }

        RecordComponent[] components = type.getRecordComponents();
        Object[]          values     = new Object[components.length];
        Class<?>[]        types      = new Class<?>[components.length];

        for (int index = 0; index < components.length; index++) {
            types[index] = components[index].getType();
            values[index] = pick(row, components[index].getName());
        }

        try {
            return type.getDeclaredConstructor(types).newInstance(values);
        } catch (ReflectiveOperationException failed) {
            throw new IllegalStateException(
                    "a row could not be read as %s: %s".formatted(type.getSimpleName(), failed.getMessage()),
                    failed);
        }
    }

    private static Object pick(Map<String, Object> row, String component) {
        for (Map.Entry<String, Object> cell : row.entrySet()) {
            if (cell.getKey().replace("_", "").equalsIgnoreCase(component)) {
                return cell.getValue();
            }
        }

        return null;
    }

    private List<Map<String, Object>> rows(Fragment statement) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement prepared = connection.prepareStatement(statement.sql())) {

            List<Object> parameters = statement.parameters();

            for (int index = 0; index < parameters.size(); index++) {
                prepared.setObject(index + 1, parameters.get(index));
            }

            try (ResultSet answered = prepared.executeQuery()) {
                ResultSetMetaData         metadata  = answered.getMetaData();
                List<Map<String, Object>> collected = new ArrayList<>();

                while (answered.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();

                    for (int column = 1; column <= metadata.getColumnCount(); column++) {
                        row.put(metadata.getColumnLabel(column), answered.getObject(column));
                    }

                    collected.add(row);
                }

                return collected;
            }
        } catch (Exception failed) {
            throw new IllegalStateException("this query could not be run: " + failed.getMessage(), failed);
        }
    }

    /** The engine behind this factory, for a caller that wants to compile something itself. */
    public QueryEngine engine() {
        return engine;
    }
}
