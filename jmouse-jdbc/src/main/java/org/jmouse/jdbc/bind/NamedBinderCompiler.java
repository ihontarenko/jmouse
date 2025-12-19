package org.jmouse.jdbc.bind;

import org.jmouse.jdbc.statement.PreparedStatementBinder;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public final class NamedBinderCompiler {

    private final MissingParameterPolicy missingPolicy;

    public NamedBinderCompiler(MissingParameterPolicy missingPolicy) {
        this.missingPolicy = missingPolicy;
    }

    public NamedSQL compile(String rawSQL) {
        ParsedSQL parsed = new NamedParameterParser().parse(rawSQL);
        return new NamedSQL(rawSQL, parsed.sql(), parsed.parameters());
    }

    public PreparedStatementBinder binder(NamedSQL sql, ParameterSource source) {
        return statement -> bindAll(statement, sql.parameters(), source);
    }

    private void bindAll(PreparedStatement statement, List<String> names, ParameterSource source) throws SQLException {
        for (int i = 0; i < names.size(); i++) {
            String name  = names.get(i);
            int    index = i + 1;

            if (!source.hasValue(name)) {
                if (missingPolicy == MissingParameterPolicy.FAIL_FAST) {
                    throw new IllegalArgumentException("Missing SQL parameter '" + name + "'");
                }
                statement.setObject(index, null);
                continue;
            }

            Object value = source.getValue(name);
            statement.setObject(index, value);
        }
    }
}
