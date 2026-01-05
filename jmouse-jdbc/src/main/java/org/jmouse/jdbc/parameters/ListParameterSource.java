package org.jmouse.jdbc.parameters;

import java.util.List;
import java.util.Objects;

/**
 * {@link ParameterSource} implementation backed by a {@link List} of positional values.
 * <p>
 * {@code ListParameterSource} provides values for <b>positional</b> SQL parameters
 * using a {@link List} instead of a raw array.
 *
 * <p>
 * JDBC parameter positions are interpreted as:
 * <ul>
 *     <li>1-based positions in SQL</li>
 *     <li>0-based indexes in the backing list</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * List<Object> params = List.of(10L, true);
 * ParameterSource source = new ListParameterSource(params);
 *
 * // SQL: "select * from users where id = ? and active = ?"
 * }</pre>
 *
 * <p>
 * Named parameters are explicitly unsupported and will result in
 * {@link UnsupportedOperationException}.
 *
 * @author jMouse
 */
public final class ListParameterSource implements ParameterSource {

    /**
     * Backing list holding positional parameter values.
     */
    private final List<?> parameterValues;

    /**
     * Creates a new {@code ListParameterSource}.
     *
     * @param parameterValues list of parameter values
     */
    public ListParameterSource(List<?> parameterValues) {
        this.parameterValues = Objects.requireNonNull(parameterValues, "parameterValues");
    }

    /**
     * Checks whether a value exists for the given positional parameter.
     *
     * @param position 1-based JDBC parameter position
     * @return {@code true} if a value is present
     */
    @Override
    public boolean hasValue(int position) {
        int index = position - 1;
        return index >= 0 && index < parameterValues.size();
    }

    /**
     * Always returns {@code false} as named parameters are not supported.
     *
     * @param name parameter name
     * @return {@code false}
     */
    @Override
    public boolean hasValue(String name) {
        return false;
    }

    /**
     * Returns the value for the given positional parameter.
     *
     * @param position 1-based JDBC parameter position
     * @return parameter value
     */
    @Override
    public Object getValue(int position) {
        return parameterValues.get(position - 1);
    }

    /**
     * Named parameters are not supported by this source.
     *
     * @param name parameter name
     * @throws UnsupportedOperationException always
     */
    @Override
    public Object getValue(String name) {
        throw new UnsupportedOperationException("Named parameters are not supported by ListParameterSource");
    }
}
