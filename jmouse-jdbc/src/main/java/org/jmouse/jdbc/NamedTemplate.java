package org.jmouse.jdbc;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.jdbc.parameters.MissingParameterPolicy;
import org.jmouse.jdbc.parameters.ParameterSource;
import org.jmouse.jdbc.mapping.KeyExtractor;
import org.jmouse.jdbc.mapping.RowMapper;
import org.jmouse.jdbc.parameters.SQLCompiled;
import org.jmouse.jdbc.parameters.SQLParameterProcessor;
import org.jmouse.jdbc.parameters.SQLParsed;
import org.jmouse.jdbc.parameters.bind.SQLPlanPreparedStatementBinder;
import org.jmouse.jdbc.statement.PreparedStatementBinder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class NamedTemplate extends SimpleTemplate implements NamedOperations {

    private final ConcurrentHashMap<String, SQLCompiled> cache = new ConcurrentHashMap<>();
    private final ExpressionLanguage expressionLanguage;
    private final SQLParameterProcessor                  processor;
    private final MissingParameterPolicy                 missingPolicy;

    public NamedTemplate(
            JdbcExecutor executor,
            ExpressionLanguage expressionLanguage,
            SQLParameterProcessor processor,
            MissingParameterPolicy missingPolicy
    ) {
        super(executor);
        this.expressionLanguage = expressionLanguage;
        this.processor = processor;
        this.missingPolicy = missingPolicy;
    }

    @Override
    public <T> Optional<T> querySingle(String sql, ParameterSource params, RowMapper<T> mapper) throws SQLException {
        SQLCompiled             compiled = compile(sql);
        PreparedStatementBinder binder   = binder(compiled, params);
        return super.querySingle(compiled.compiled(), binder, mapper);
    }

    @Override
    public <T> T queryOne(String sql, ParameterSource params, RowMapper<T> mapper) throws SQLException {
        SQLCompiled             compiled = compile(sql);
        PreparedStatementBinder binder   = binder(compiled, params);
        return super.queryOne(compiled.compiled(), binder, mapper);
    }

    @Override
    public <T> List<T> query(String sql, ParameterSource params, RowMapper<T> mapper) throws SQLException {
        SQLCompiled             compiled = compile(sql);
        PreparedStatementBinder binder   = binder(compiled, params);
        return super.query(compiled.compiled(), binder, mapper);
    }

    @Override
    public int update(String sql, ParameterSource params) throws SQLException {
        SQLCompiled             compiled = compile(sql);
        PreparedStatementBinder binder   = binder(compiled, params);
        return super.update(compiled.compiled(), binder);
    }

    @Override
    public int[] batch(String sql, List<? extends ParameterSource> parameterSources) throws SQLException {
        SQLCompiled                   compiled = compile(sql);
        List<PreparedStatementBinder> binders  = new ArrayList<>(parameterSources.size());

        for (ParameterSource parameterSource : parameterSources) {
            binders.add(binder(compiled, parameterSource));
        }

        return super.batchUpdate(compiled.compiled(), binders);
    }

    @Override
    public <K> K update(String sql, ParameterSource parameters, KeyExtractor<K> extractor) throws SQLException {
        SQLCompiled compiled = compile(sql);
        PreparedStatementBinder binder = binder(compiled, parameters);
        return super.update(compiled.compiled(), binder, extractor);
    }

    private SQLCompiled compile(String sql) {
        return cache.computeIfAbsent(sql, key -> {
            SQLParsed parsed = processor.parse("SQL", key);
            return processor.compile(parsed);
        });
    }

    private PreparedStatementBinder binder(SQLCompiled compiled, ParameterSource params) {
        return new SQLPlanPreparedStatementBinder(expressionLanguage, compiled.plan(), params, missingPolicy);
    }
}
