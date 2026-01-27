package org.jmouse.jdbc.mapping;

import org.jmouse.core.Verify;
import org.jmouse.core.bind.*;
import org.jmouse.core.bind.accessor.DummyObjectAccessor;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.jdbc.mapping.bind.JdbcAccessorWrapper;

import java.sql.ResultSet;

/**
 * {@link RowMapper} implementation that maps a JDBC {@link ResultSet} row into a Java bean
 * using the jMouse core {@link Binder} infrastructure.
 * <p>
 * The mapping process adapts the {@link ResultSet} into an {@link ObjectAccessor} via
 * {@link JdbcAccessorWrapper} and then delegates the binding into the target type using
 * {@link TypedValue} metadata (built from {@link InferredType}).
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * BeanRowMapper<User> mapperProvider = BeanRowMapper.of(User.class);
 *
 * List<User> users = jdbc.query(
 *     "select id, name, active from users",
 *     mapperProvider
 * );
 * }</pre>
 *
 * <h3>Binding expectations</h3>
 * <ul>
 *     <li>The binder must be able to instantiate the target type (constructor / factory rules)</li>
 *     <li>Column names/labels should match bindable property names (or binder must support aliases)</li>
 *     <li>Type conversion is performed by the binder conversion pipeline (if configured)</li>
 * </ul>
 *
 * <p>
 * ⚠️ This mapperProvider is stateful: it reuses a single {@link Binder} instance and swaps its
 * {@link ObjectAccessor} per row. Do not share the same instance across concurrent threads
 * unless {@link Binder} is explicitly thread-safe.
 *
 * @param <T> target bean type
 *
 * @author jMouse
 */
public final class BeanRowMapper<T> implements RowMapper<T> {

    /**
     * Target bean type.
     */
    private final Class<T> type;

    /**
     * jMouse binder used to bind values from the {@link ResultSet}-backed accessor into the target bean.
     */
    private final Binder binder;

    /**
     * Creates a new {@code BeanRowMapper} for the given type.
     *
     * @param type target bean type (must not be {@code null})
     */
    public BeanRowMapper(Class<T> type) {
        this.type = Verify.nonNull(type, "type");
        this.binder = new Binder(new DummyObjectAccessor(null));
    }

    /**
     * Factory method for creating a {@code BeanRowMapper}.
     *
     * @param type target bean type
     * @param <T>  target type
     * @return mapperProvider instance
     */
    public static <T> BeanRowMapper<T> of(Class<T> type) {
        return new BeanRowMapper<>(type);
    }

    /**
     * Maps the current {@link ResultSet} row into an instance of {@code T}.
     * <p>
     * The {@link ResultSet} is wrapped into an {@link ObjectAccessor} and then bound
     * into the target type using the configured {@link Binder}.
     *
     * @param resultSet current result set (cursor positioned on a valid row)
     * @param rowIndex  zero-based row index (framework-provided)
     * @return mapped bean instance
     * @throws IllegalStateException if binding produced no value
     */
    @Override
    public T map(ResultSet resultSet, int rowIndex) {
        ObjectAccessor accessor = JdbcAccessorWrapper.WRAPPER.wrap(resultSet);
        binder.setObjectAccessor(accessor);
        BindResult<T> result = binder.bind(null, toBindable(type));
        Verify.state(result.isPresent(), "Failed to map bean-type: '" + type + "'.");
        return result.getValue();
    }

    /**
     * Converts a class into a {@link TypedValue} representation used by the binder.
     *
     * @param type target type
     * @return bindable metadata for the target type
     */
    private TypedValue<T> toBindable(Class<T> type) {
        return TypedValue.of(InferredType.forClass(type));
    }

}
