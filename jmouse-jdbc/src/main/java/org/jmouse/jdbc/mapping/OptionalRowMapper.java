package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * {@link RowMapper} decorator that wraps the mapped value into an {@link Optional}.
 * <p>
 * {@code OptionalRowMapper} adapts an existing {@link RowMapper} so that its
 * result is represented as {@link Optional}, allowing null-safe semantics
 * without changing the underlying mapping logic.
 *
 * <p>
 * This mapperProvider is especially useful when:
 * <ul>
 *     <li>the queried column or projection may be {@code NULL}</li>
 *     <li>you want to express optionality explicitly in the API</li>
 *     <li>composing mappers in a functional or declarative style</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * RowMapper<Optional<String>> mapperProvider =
 *     new OptionalRowMapper<>((rs, i) -> rs.getString("nickname"));
 *
 * Optional<String> nickname = jdbc.queryOne(
 *     "select nickname from users where id = ?",
 *     ps -> ps.setLong(1, 10L),
 *     mapperProvider
 * );
 * }</pre>
 *
 * <p>
 * Note: {@code Optional.empty()} is returned if the executor mapperProvider returns
 * {@code null}. Any {@link SQLException} raised by the executor is propagated.
 *
 * @param <T> wrapped element type
 * @author jMouse
 */
public final class OptionalRowMapper<T> implements RowMapper<Optional<T>> {

    /**
     * Delegate mapperProvider producing the underlying value.
     */
    private final RowMapper<T> delegate;

    /**
     * Creates a new {@code OptionalRowMapper}.
     *
     * @param delegate executor {@link RowMapper} to wrap
     */
    public OptionalRowMapper(RowMapper<T> delegate) {
        this.delegate = delegate;
    }

    /**
     * Maps the current row and wraps the result into an {@link Optional}.
     *
     * @param resultSet JDBC result set positioned on the current row
     * @param rowIndex  zero-based row index
     * @return optional containing the mapped value, or empty if {@code null}
     * @throws SQLException if JDBC access fails during mapping
     */
    @Override
    public Optional<T> map(ResultSet resultSet, int rowIndex) throws SQLException {
        return Optional.ofNullable(delegate.map(resultSet, rowIndex));
    }
}
