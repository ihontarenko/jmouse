package org.jmouse.jdbc.operation.execution;

import org.jmouse.core.Verify;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.jdbc.parameters.*;
import org.jmouse.jdbc.parameters.bind.SQLPlanPreparedStatementBinder;
import org.jmouse.jdbc.statement.StatementBinder;

import java.util.concurrent.ConcurrentHashMap;

import static org.jmouse.core.Verify.nonNull;

/**
 * Factory for preparing executable SQL from named SQL + parameters.
 *
 * <p>Extracted from {@code NamedTemplate} to be reused in operation-based execution.</p>
 */
public class NamedSqlPreparedExecutionFactory {

    private final ConcurrentHashMap<String, SQLCompiled> cache = new ConcurrentHashMap<>();

    private final ExpressionLanguage     expressionLanguage;
    private final SQLParameterProcessor  processor;
    private final MissingParameterPolicy missingPolicy;

    public NamedSqlPreparedExecutionFactory(
            ExpressionLanguage expressionLanguage, SQLParameterProcessor processor, MissingParameterPolicy missingPolicy
    ) {
        this.expressionLanguage = nonNull(expressionLanguage, "expressionLanguage");
        this.processor = nonNull(processor, "processor");
        this.missingPolicy = nonNull(missingPolicy, "missingPolicy");
    }

    /**
     * Prepares SQL and binder for execution.
     *
     * @param sql        raw SQL with named parameters
     * @param parameters parameter source
     * @return prepared execution descriptor
     */
    public NamedSqlPreparedExecution prepare(String sql, ParameterSource parameters) {
        SQLCompiled     compiled = compile(sql);
        StatementBinder binder   = binder(compiled, parameters);

        return new NamedSqlPreparedExecution(compiled.compiled(), binder);
    }

    private SQLCompiled compile(String sql) {
        return cache.computeIfAbsent(sql, key -> {
            SQLParsed parsed = processor.parse("SQL", key);
            return processor.compile(parsed);
        });
    }

    private StatementBinder binder(SQLCompiled compiled, ParameterSource parameters) {
        return new SQLPlanPreparedStatementBinder(expressionLanguage, compiled.plan(), parameters, missingPolicy);
    }

}