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

    public static StatementBinder empty() {
        return stmt -> {};
    }

    public static <T> List<StatementBinder> forEach(List<T> items, BinderFactory<T> factory) {
        Contract.nonNull(items, "items");
        Contract.nonNull(factory, "factory");

        List<StatementBinder> out = new ArrayList<>(items.size());
        for (T item : items) {
            out.add(factory.binderFor(item));
        }
        return out;
    }

    public static <T> List<StatementBinder> forEach(List<T> items, BinderFactory<T> factory) {
        Contract.nonNull(items, "items");
        Contract.nonNull(factory, "factory");

        if (items.isEmpty()) {
            return List.of();
        }

        List<StatementBinder> binders = new ArrayList<>(items.size());

        for (T item : items) {
            binders.add(factory.binderFor(item));
        }

        return binders;
    }

    public static StatementBinder ofObjects(Object... arguments) {
        return statement -> {
            for (int index = 0; index < arguments.length; index++) {
                statement.setObject(index + 1, arguments[index]);
            }
        };
    }

    public static StatementBinder bindString(int index, String value) {
        return statement -> statement.setString(index, value);
    }

    public static StatementBinder bindLong(int index, long value) {
        return statement -> statement.setLong(index, value);
    }

    public static StatementBinder bindLong(int index, Long value) {
        return statement -> {
            if (value == null) {
                statement.setObject(index, null);
            } else {
                statement.setLong(index, value);
            }
        };
    }

    public static StatementBinder bindInteger(int index, int value) {
        return statement -> statement.setInt(index, value);
    }

    public static StatementBinder bindBoolean(int index, boolean value) {
        return statement -> statement.setBoolean(index, value);
    }

    public static StatementBinder bindInstant(int index, Instant value) {
        return statement -> statement.setTimestamp(index, value == null ? null : Timestamp.from(value));
    }

    public static StatementBinder composeBinders(StatementBinder... binders) {
        return statement -> {
            for (StatementBinder binder : binders) {
                if (binder != null) {
                    binder.bind(statement);
                }
            }
        };
    }

    public static StatementBinder checked(CheckedBinder binder) {
        return binder::bind;
    }

    @FunctionalInterface
    public interface CheckedBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}