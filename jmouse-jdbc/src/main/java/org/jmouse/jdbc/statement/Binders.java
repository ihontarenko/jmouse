package org.jmouse.jdbc.statement;

import org.jmouse.core.Contract;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Binders {

    private Binders() {}

    public static PreparedStatementBinder empty() {
        return stmt -> {};
    }

    public static <T> List<PreparedStatementBinder> forEach(List<T> items, BinderFactory<T> factory) {
        Contract.nonNull(items, "items");
        Contract.nonNull(factory, "factory");

        if (items.isEmpty()) {
            return List.of();
        }

        List<PreparedStatementBinder> binders = new ArrayList<>(items.size());

        for (T item : items) {
            binders.add(factory.binderFor(item));
        }

        return binders;
    }

    public static PreparedStatementBinder ofObjects(Object... arguments) {
        return statement -> {
            for (int index = 0; index < arguments.length; index++) {
                statement.setObject(index + 1, arguments[index]);
            }
        };
    }

    public static PreparedStatementBinder bindString(int index, String value) {
        return statement -> statement.setString(index, value);
    }

    public static PreparedStatementBinder bindLong(int index, long value) {
        return statement -> statement.setLong(index, value);
    }

    public static PreparedStatementBinder bindLong(int index, Long value) {
        return statement -> {
            if (value == null) {
                statement.setObject(index, null);
            } else {
                statement.setLong(index, value);
            }
        };
    }

    public static PreparedStatementBinder bindInteger(int index, int value) {
        return statement -> statement.setInt(index, value);
    }

    public static PreparedStatementBinder bindBoolean(int index, boolean value) {
        return statement -> statement.setBoolean(index, value);
    }

    public static PreparedStatementBinder bindInstant(int index, Instant value) {
        return statement -> statement.setTimestamp(index, value == null ? null : Timestamp.from(value));
    }

    public static PreparedStatementBinder composeBinders(PreparedStatementBinder... binders) {
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