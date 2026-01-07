package org.jmouse.jdbc.parameters.bind;

import org.jmouse.core.Verify;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Expression;
import org.jmouse.jdbc.parameters.MissingParameterPolicy;
import org.jmouse.jdbc.parameters.ParameterSource;
import org.jmouse.jdbc.parameters.SQLPlan;
import org.jmouse.jdbc.parameters.SQLPlan.Binding;
import org.jmouse.jdbc.parameters.SQLPlan.Binding.Kind;
import org.jmouse.jdbc.statement.StatementBinder;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static java.lang.String.valueOf;

/**
 * {@link StatementBinder} implementation that binds parameters according to a precompiled {@link SQLPlan}.
 * <p>
 * {@code SQLPlanPreparedStatementBinder} is the bridge between:
 * <ul>
 *     <li>a parsed SQL template plan ({@link SQLPlan})</li>
 *     <li>a runtime parameter model ({@link ParameterSource})</li>
 *     <li>optional value transformations via {@link ExpressionLanguage}</li>
 * </ul>
 *
 * <h3>How it works</h3>
 * <ol>
 *     <li>Iterates {@link SQLPlan#bindings()} in order</li>
 *     <li>Resolves each binding value from {@link ParameterSource}</li>
 *     <li>Optionally applies a binding expression (if present)</li>
 *     <li>Writes the final value into the {@link PreparedStatement} via {@link PreparedStatement#setObject(int, Object)}</li>
 * </ol>
 *
 * <h3>Missing parameter policy</h3>
 * <ul>
 *     <li>{@link MissingParameterPolicy#BIND_NULL}: binds {@code null}</li>
 *     <li>{@link MissingParameterPolicy#FAIL_FAST}: throws {@link IllegalArgumentException}</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * SQLPlan plan = planner.compile("select * from users where id = :id");
 * ParameterSource parameters = ParameterSource.of(Map.of("id", 10L));
 *
 * StatementBinder binder = new SQLPlanPreparedStatementBinder(
 *     expressionLanguage,
 *     plan,
 *     parameters,
 *     MissingParameterPolicy.FAIL_FAST
 * );
 *
 * executor.execute(
 *     plan.sql(),
 *     binder,
 *     StatementConfigurer.NOOP,
 *     StatementHandler.NOOP,
 *     StatementCallback.QUERY,
 *     extractor
 * );
 * }</pre>
 *
 * <p>
 * Note: Expressions are evaluated with a fresh {@link EvaluationContext} per binding.
 * The resolved parameter name is inserted into the context using {@link EvaluationContext#setValue(String, Object)}.
 *
 * @author jMouse
 */
public final class SQLPlanPreparedStatementBinder implements StatementBinder {

    private final ExpressionLanguage     expressionLanguage;
    private final SQLPlan                plan;
    private final ParameterSource        parameterSource;
    private final MissingParameterPolicy missingPolicy;

    /**
     * Creates a new binder for the given plan and parameter source.
     *
     * @param expressionLanguage expression language used for optional value transformations
     * @param plan               precompiled SQL plan containing binding descriptors
     * @param parameterSource    parameter source used to resolve named/positional values
     * @param missingPolicy      missing-parameter handling policy
     */
    public SQLPlanPreparedStatementBinder(
            ExpressionLanguage expressionLanguage, SQLPlan plan, ParameterSource parameterSource, MissingParameterPolicy missingPolicy) {
        this.expressionLanguage = Verify.nonNull(expressionLanguage, "expressionLanguage");
        this.plan = Verify.nonNull(plan, "plan");
        this.parameterSource = Verify.nonNull(parameterSource, "parameterSource");
        this.missingPolicy = Verify.nonNull(missingPolicy, "missingPolicy");
    }

    /**
     * Binds plan parameters into the provided {@link PreparedStatement}.
     *
     * @param statement prepared statement to bind
     * @throws SQLException if JDBC access fails during binding
     * @throws IllegalArgumentException if a parameter is missing and {@link MissingParameterPolicy#FAIL_FAST} is used
     */
    @Override
    public void bind(PreparedStatement statement) throws SQLException {
        int parameterPosition = 1;

        for (Binding binding : plan.bindings()) {
            Object value;

            if (binding instanceof Binding.Named(String name, String rawToken, Expression expression, Kind kind)) {
                value = !parameterSource.hasValue(name)
                        ? handleMissing(kind.name(), name, parameterPosition) : parameterSource.getValue(name);
                if (expression != null) {
                    EvaluationContext evaluationContext = expressionLanguage.newContext();
                    evaluationContext.setValue(name, value);
                    value = expression.evaluate(evaluationContext);
                }
            } else if (binding instanceof Binding.Positional(int position, Kind kind)) {
                value = !parameterSource.hasValue(position)
                        ? handleMissing(kind.name(), valueOf(position), parameterPosition) : parameterSource.getValue(position);
            } else {
                throw new IllegalStateException("Unknown binding: " + binding);
            }

            statement.setObject(parameterPosition++, value);
        }
    }

    /**
     * Applies the configured {@link MissingParameterPolicy} when a parameter is absent.
     *
     * @param kind     binding kind (named/positional)
     * @param key      parameter key (name or position)
     * @param jdbcIndex 1-based JDBC parameter index in the prepared statement
     * @return value to bind (may be {@code null})
     * @throws IllegalArgumentException if policy is {@link MissingParameterPolicy#FAIL_FAST}
     */
    private Object handleMissing(String kind, String key, int jdbcIndex) {
        return switch (missingPolicy) {
            case BIND_NULL -> null;
            case FAIL_FAST -> throw new IllegalArgumentException(
                    "Missing %s parameter '%s' for JDBC index ?%d".formatted(kind, key, jdbcIndex)
            );
        };
    }
}
