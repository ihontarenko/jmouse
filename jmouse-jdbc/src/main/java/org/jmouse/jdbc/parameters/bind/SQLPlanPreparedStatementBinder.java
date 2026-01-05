package org.jmouse.jdbc.parameters.bind;

import org.jmouse.core.Contract;
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

public final class SQLPlanPreparedStatementBinder implements StatementBinder {

    private final ExpressionLanguage     expressionLanguage;
    private final SQLPlan                plan;
    private final ParameterSource        parameterSource;
    private final MissingParameterPolicy missingPolicy;

    public SQLPlanPreparedStatementBinder(
            ExpressionLanguage expressionLanguage, SQLPlan plan, ParameterSource parameterSource, MissingParameterPolicy missingPolicy) {
        this.expressionLanguage = Contract.nonNull(expressionLanguage, "expressionLanguage");
        this.plan = Contract.nonNull(plan, "plan");
        this.parameterSource = Contract.nonNull(parameterSource, "parameterSource");
        this.missingPolicy = Contract.nonNull(missingPolicy, "missingPolicy");
    }

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

    private Object handleMissing(String kind, String key, int jdbcIndex) {
        return switch (missingPolicy) {
            case BIND_NULL -> null;
            case FAIL_FAST -> throw new IllegalArgumentException(
                    "Missing %s parameter '%s' for JDBC index ?%d".formatted(kind, key, jdbcIndex)
            );
        };
    }
}
