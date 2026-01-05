package org.jmouse.jdbc.statement;

import java.sql.SQLException;
import java.sql.Statement;

@FunctionalInterface
public interface StatementHandler {

    <S extends Statement, R> R handle(S statement, StatementExecutor<? super S, R> executor) throws SQLException;

    @FunctionalInterface
    interface StatementExecutor<S extends Statement, R> {
        R execute(S statement) throws SQLException;
    }

    StatementHandler NOOP = new StatementHandler() {
        @Override
        public <S extends Statement, R> R handle(S statement, StatementExecutor<? super S, R> executor) throws SQLException {
            return executor.execute(statement);
        }

        @Override
        public String toString() {
            return "NOOP";
        }
    };

    static StatementHandler chain(StatementHandler h0, StatementHandler h1) {
        h0 = h0 == null ? NOOP : h0;
        h1 = h1 == null ? NOOP : h1;

        StatementHandler temporary0 = h0;
        StatementHandler temporary1 = h1;

        return new StatementHandler() {
            @Override
            public <S extends Statement, R> R handle(S statement, StatementExecutor<? super S, R> executor) throws SQLException {
                return temporary0.handle(statement, s -> temporary1.handle(s, executor));
            }
            @Override
            public String toString() {
                return "CHAINED: (%s && %s)".formatted(temporary0, h1);
            }
        };
    }
}