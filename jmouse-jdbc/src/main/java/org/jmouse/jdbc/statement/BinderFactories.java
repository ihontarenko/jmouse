package org.jmouse.jdbc.statement;

import org.jmouse.core.Getter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.jmouse.core.Verify.nonNull;

public final class BinderFactories {

    private BinderFactories() {}

    public static <T> BinderFactory<T> tuple(TupleBinder<T> binder) {
        return item -> Binders.checked(statement -> nonNull(binder, "binder").bind(statement, item));
    }

    public static <T> Columns<T> columns() {
        return new Columns<>(null);
    }

    public static <T> BeanBinder<T> bean(Class<T> type) {
        return new BeanBinder<>(type);
    }

    public static final class BeanBinder<T> {
        private final Class<T> type;

        private BeanBinder(Class<T> type) {
            this.type = nonNull(type, "type");
        }

        public Columns<T> columns() {
            return new Columns<>(type);
        }

        public <V> Columns<T> map(String name, Getter<T, V> getter, ParameterBinder<V> binder) {
            return columns().map(name, getter, binder);
        }

        public Columns<T> bindString(String name, Getter<T, String> getter) {
            return columns().string(name, getter);
        }

        public Columns<T> bindInteger(String name, Getter<T, Integer> getter) {
            return columns().map(name, getter, ParameterBinder.integerBinder());
        }

        public Columns<T> bindObject(String name, Getter<T, Long> getter) {
            return columns().map(name, getter, ParameterBinder.longBinder());
        }
    }

    public static final class Columns<T> implements BinderFactory<T> {

        private final Class<?>           modelType; // optional meta
        private final List<Column<T, ?>> columns = new ArrayList<>();

        private Columns(Class<?> modelType) {
            this.modelType = modelType;
        }

        public <V> Columns<T> map(String name, Getter<T, V> getter, ParameterBinder<V> binder) {
            columns.add(new NamedColumn<>(
                    nonNull(name, "name"),
                    nonNull(getter, "getter"),
                    nonNull(binder, "binder"))
            );
            return this;
        }

        public <V> Columns<T> map(Getter<T, V> getter, ParameterBinder<V> binder) {
            columns.add(new UnnamedColumn<>(
                    nonNull(getter, "getter"),
                    nonNull(binder, "binder"))
            );
            return this;
        }

        public Columns<T> string(String name, Getter<T, String> getter) {
            return map(name, getter, ParameterBinder.stringBinder());
        }

        public List<String> columnNames() {
            List<String> result = new ArrayList<>(columns.size());
            for (Column<T, ?> column : columns) {
                result.add(column.columnName().orElse(null));
            }
            return Collections.unmodifiableList(result);
        }

        public int parameterCount() {
            return columns.size();
        }

        @Override
        public StatementBinder binderFor(T item) {
            return Binders.checked(ps -> bindAll(ps, item, 1));
        }

        public StatementBinder binderFor(T item, int startIndex) {
            return Binders.checked(ps -> bindAll(ps, item, startIndex));
        }

        private void bindAll(PreparedStatement ps, T item, int index) throws SQLException {
            for (Column<T, ?> column : columns) {
                try {
                    index = column.bind(ps, index, item);
                } catch (SQLException e) {
                    throw enrich(e, column, index, item);
                }
            }
        }

        private SQLException enrich(SQLException exception, Column<T, ?> column, int i, T instance) {
            String m = (modelType != null ? modelType.getSimpleName() : "<?>");
            String c = column.columnName().orElse("#" + i);
            return new SQLException("Bind failed: n=%s, column=%s, index=%d".formatted(m, c, i),
                                    exception.getSQLState(), exception.getErrorCode(), exception);
        }

        private sealed interface Column<T, V> permits NamedColumn, UnnamedColumn {

            default Optional<String> columnName() {
                return Optional.empty();
            }

            int bind(PreparedStatement ps, int index, T item) throws SQLException;

        }

        private record NamedColumn<T, V>(String name, Getter<T, V> getter, ParameterBinder<V> binder)
                implements Column<T, V> {

            @Override
            public Optional<String> columnName() {
                return Optional.of(name());
            }

            @Override
            public int bind(PreparedStatement statement, int index, T item) throws SQLException {
                V value = getter.get(item);
                binder.bind(statement, index, value);
                return index + 1;
            }

        }

        private record UnnamedColumn<T, V>(Getter<T, V> getter, ParameterBinder<V> binder)
                implements Column<T, V> {

            @Override
            public int bind(PreparedStatement statement, int index, T item) throws SQLException {
                V value = getter.get(item);
                binder.bind(statement, index, value);
                return index + 1;
            }

        }
    }

}