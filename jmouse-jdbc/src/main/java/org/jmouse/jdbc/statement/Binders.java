package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public final class Binders {

    private Binders() {}

    public static PreparedStatementBinder empty() {
        return stmt -> {};
    }

    public static PreparedStatementBinder of(Object... arguments) {
        return statement -> {
            for (int i = 0; i < arguments.length; i++) {
                statement.setObject(i + 1, arguments[i]);
            }
        };
    }

    public static PreparedStatementBinder string(int index, String value) {
        return stmt -> stmt.setString(index, value);
    }

    public static PreparedStatementBinder longValue(int index, long value) {
        return stmt -> stmt.setLong(index, value);
    }

    public static PreparedStatementBinder longObject(int index, Long value) {
        return statement -> {
            if (value == null) {
                statement.setObject(index, null);
            } else {
                statement.setLong(index, value);
            }
        };
    }

    public static PreparedStatementBinder intValue(int index, int value) {
        return statement -> statement.setInt(index, value);
    }

    public static PreparedStatementBinder boolValue(int index, boolean value) {
        return statement -> statement.setBoolean(index, value);
    }

    public static PreparedStatementBinder instant(int index, Instant value) {
        return statement -> statement.setTimestamp(index, value == null ? null : Timestamp.from(value));
    }

    public static PreparedStatementBinder compose(PreparedStatementBinder... binders) {
        return statement -> {
            for (PreparedStatementBinder binder : binders) {
                if (binder != null) {
                    binder.bind(statement);
                }
            }
        };
    }

    public static PreparedStatementBinder checked(CheckedBinder binder) {
        return binder::bind;
    }

    @FunctionalInterface
    public interface CheckedBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}