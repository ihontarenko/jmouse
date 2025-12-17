package org.jmouse.jdbc.bind;

import org.jmouse.jdbc.statement.PreparedStatementBinder;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public final class NamedBinderCompiler {

    private final MissingParameterPolicy missingPolicy;

    public NamedBinderCompiler(MissingParameterPolicy missingPolicy) {
        this.missingPolicy = missingPolicy;
    }

    public CompiledNamedSQL compile(String namedSql) {
        ParsedSQL parsed = new NamedParameterParser().parse(namedSql);
        return new CompiledNamedSQL(namedSql, parsed.sql(), parsed.parameters());
    }

    public PreparedStatementBinder binder(CompiledNamedSQL sql, ParameterSource source) {
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(source, "source");

        List<String> names = sql.parameters();
        return stmt -> bindAll(stmt, names, source);
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
