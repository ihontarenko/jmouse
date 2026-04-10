package org.jmouse.jdbc.parameters.named;

import org.jmouse.jdbc.parameters.*;
import org.jmouse.jdbc.parameters.bind.SQLPlanPreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementBinder;

import java.util.concurrent.ConcurrentHashMap;

import static org.jmouse.core.Verify.nonNull;

/**
 * Factory for preparing executable SQL from named SQL + parameters.
 *
 * <p>Performs parse → compile → bind pipeline with internal caching of compiled SQL.</p>
 */
public class NamedSqlPreparedExecutionFactory {

    private final SQLParameterProcessor                  processor;
    private final MissingParameterPolicy                 missingPolicy;
    private final ConcurrentHashMap<String, SQLCompiled> cache = new ConcurrentHashMap<>();

    /**
     * Creates a factory with required processing components.
     *
     * @param processor SQL parameter processor (parse + compile)
     * @param missingPolicy policy for handling missing parameters
     */
    public NamedSqlPreparedExecutionFactory(
            SQLParameterProcessor processor, MissingParameterPolicy missingPolicy
    ) {
        this.processor = nonNull(processor, "processor");
        this.missingPolicy = nonNull(missingPolicy, "missingPolicy");
    }

    /**
     * Prepares SQL and binder for execution.
     *
     * <p>Returns compiled SQL (with {@code ?} placeholders) and a {@link StatementBinder}
     * that applies parameter values according to the compiled plan.</p>
     *
     * @param sql raw SQL with named parameters
     * @param parameters parameter source
     * @return prepared execution descriptor
     */
    public NamedSqlPreparedExecution prepare(String sql, ParameterSource parameters) {
        SQLCompiled     compiled = compile(sql);
        StatementBinder binder   = binder(compiled, parameters);
        return new NamedSqlPreparedExecution(compiled.compiled(), binder);
    }

    /**
     * Compiles SQL with caching.
     *
     * <p>Compilation is performed once per unique SQL string.</p>
     *
     * @param sql raw SQL
     * @return compiled SQL representation
     */
    public SQLCompiled compile(String sql) {
        return cache.computeIfAbsent(sql, q -> {
            SQLParsed parsed = processor.parse("SQL_QUERY", q);
            return processor.compile(parsed);
        });
    }

    /**
     * Creates a binder for applying parameters to a prepared statement.
     *
     * @param compiled compiled SQL
     * @param parameters parameter source
     * @return statement binder
     */
    public StatementBinder binder(SQLCompiled compiled, ParameterSource parameters) {
        return new SQLPlanPreparedStatementBinder(compiled.plan(), parameters, missingPolicy);
    }

}