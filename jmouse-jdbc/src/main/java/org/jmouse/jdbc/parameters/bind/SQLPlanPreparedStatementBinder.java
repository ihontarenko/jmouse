package org.jmouse.jdbc.parameters.bind;

import org.jmouse.core.Contract;
import org.jmouse.el.node.Expression;
import org.jmouse.jdbc.bind.MissingParameterPolicy;
import org.jmouse.jdbc.bind.ParameterSource;
import org.jmouse.jdbc.parameters.SQLPlan;
import org.jmouse.jdbc.statement.PreparedStatementBinder;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class SQLPlanPreparedStatementBinder implements PreparedStatementBinder {

    private final SQLPlan                plan;
    private final ParameterSource        parameterSource;
    private final MissingParameterPolicy missingPolicy;

    public SQLPlanPreparedStatementBinder(
            SQLPlan plan,
            ParameterSource parameterSource,
            MissingParameterPolicy missingPolicy
    ) {
        this.plan = Contract.nonNull(plan, "plan");
        this.parameterSource = Contract.nonNull(parameterSource, "parameterSource");
        this.missingPolicy = Contract.nonNull(missingPolicy, "missingPolicy");
    }

    @Override
    public void bind(PreparedStatement statement) throws SQLException {
        int parameterPosition = 1;

        for (SQLPlan.Binding binding : plan.bindings()) {
            Object value;

            if (binding instanceof SQLPlan.Binding.Named(String name, String rawToken, Expression expression)) {
                if (!parameterSource.hasValue(name)) {
                    handleMissing("named", name, parameterPosition);
                }
                value = parameterSource.getValue(name);
                if (expression != null) {
                    value = expression.evaluate(null);
                }
            } else if (binding instanceof SQLPlan.Binding.Positional(int position)) {
                if (!parameterSource.hasValue(position)) {
                    handleMissing("positional", "?" + position, parameterPosition);
                }
                value = parameterSource.getValue(position);
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
