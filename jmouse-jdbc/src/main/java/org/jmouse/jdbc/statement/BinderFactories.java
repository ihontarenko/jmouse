package org.jmouse.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.jmouse.core.Contract.nonNull;

public final class BinderFactories {

    private BinderFactories() {}

    public static <T> BinderFactory<T> tuple(TupleBinder<T> binder) {
        return item -> Binders.checked(ps -> nonNull(binder, "binder").bind(ps, item));
    }

    public static <T> Columns<T> columns() {
        return new Columns<>();
    }

    public static final class Columns<T> implements BinderFactory<T> {

        private final List<Column<T, ?>> columns = new ArrayList<>();

        public <V> Columns<T> map(Function<T, V> getter, ParameterBinder<V> binder) {
            columns.add(new Column<>(nonNull(getter, "getter"), nonNull(binder, "binder")));
            return this;
        }

        public Columns<T> string(Function<T, String> getter) {
            return map(getter, PreparedStatement::setString);
        }

        public Columns<T> longValue(Function<T, Long> getter) {
            return map(getter, (statement, index, value) -> {
                if (value == null) {
                    statement.setObject(index, null);
                } else {
                    statement.setLong(index, value);
                }
            });
        }

        public Columns<T> intValue(Function<T, Integer> getter) {
            return map(getter, (ps, i, v) -> {
                if (v == null) ps.setObject(i, null);
                else ps.setInt(i, v);
            });
        }

        public Columns<T> object(Function<T, Object> getter) {
            return map(getter, PreparedStatement::setObject);
        }

        @Override
        public PreparedStatementBinder binderFor(T item) {
            return Binders.checked(ps -> bindAll(ps, item));
        }

        public PreparedStatementBinder binder(T item) {
            return binderFor(item);
        }

        public PreparedStatementBinder binderFor(T item, int startIndex) {
            return Binders.checked(ps -> bindAll(ps, item, startIndex));
        }

        private void bindAll(PreparedStatement ps, T item) throws SQLException {
            bindAll(ps, item, 1);
        }

        private void bindAll(PreparedStatement statement, T item, int startIndex) throws SQLException {
            for (Column<T, ?> column : columns) {
                startIndex = column.bind(statement, startIndex, item);
            }
        }

        private record Column<T, V>(Function<T, V> getter, ParameterBinder<V> binder) {
            int bind(PreparedStatement statement, int index, T item) throws SQLException {
                V value = getter.apply(item);
                binder.bind(statement, index, value);
                return index + 1;
            }
        }
    }

}