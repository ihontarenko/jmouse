package org.jmouse.jdbc.statement;

import org.jmouse.core.Verify;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Binders {

    private Binders() {}

    public static StatementBinder empty() {
        return statement -> {};
    }

    public static <T> List<StatementBinder> forEach(List<T> items, BinderFactory<T> factory) {
        Verify.nonNull(items, "items");
        Verify.nonNull(factory, "factory");

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

    public static CountedStatementBinder countedOf(Object... arguments) {
        return CountedStatementBinder.of(ofObjects(arguments), arguments == null ? 0 : arguments.length);
    }

    public static CountedStatementBinder countedOf(CountedStatementBinder... binders) {
        return new CountedStatementBinder() {
            @Override
            public void bind(PreparedStatement statement) throws SQLException {
                for (CountedStatementBinder binder : binders) {
                    if (binder != null) {
                        binder.bind(statement);
                    }
                }
            }

            @Override
            public int countOfParameters() {
                int count = 0;

                for (CountedStatementBinder binder : binders) {
                    if (binder != null) {
                        count += binder.countOfParameters();
                    }
                }

                return count;
            }
        };
    }

    public static CountedStatementBinder counted(StatementBinder binder, int countOfParameters) {
        return CountedStatementBinder.of(binder, countOfParameters);
    }

    public static StatementBinder checked(CheckedBinder binder) {
        return binder::bind;
    }

    @FunctionalInterface
    public interface CheckedBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}