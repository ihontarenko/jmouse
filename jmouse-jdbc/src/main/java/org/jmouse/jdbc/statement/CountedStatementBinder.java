package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface CountedStatementBinder extends StatementBinder {

    int countOfParameters();

    static CountedStatementBinder of(StatementBinder binder, int countOfParameters) {
        return new CountedStatementBinder() {
            @Override
            public void bind(PreparedStatement statement) throws SQLException {
                binder.bind(statement);
            }

            @Override
            public int countOfParameters() {
                return countOfParameters;
            }

            @Override
            public String toString() {
                return "Counted(" + countOfParameters + ")";
            }
        };
    }

    static CountedStatementBinder chain(CountedStatementBinder a, CountedStatementBinder b) {
        if (a == null) return b;
        if (b == null) return a;

        return new CountedStatementBinder() {
            @Override
            public void bind(PreparedStatement statement) throws SQLException {
                a.bind(statement);
                b.bind(statement);
            }

            @Override
            public int countOfParameters() {
                return a.countOfParameters() + b.countOfParameters();
            }
        };
    }
}
