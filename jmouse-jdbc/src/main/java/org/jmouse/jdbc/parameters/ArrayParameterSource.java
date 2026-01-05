package org.jmouse.jdbc.parameters;

import java.util.Objects;

/**
 * {@link ParameterSource} implementation backed by a positional {@link Object} array.
 * <p>
 * {@code ArrayParameterSource} provides values for <b>positional</b> SQL parameters
 * (e.g. {@code ?1}, {@code ?2}, …) and does not support named parameters.
 *
 * <p>
 * Parameter positions are interpreted using JDBC semantics:
 * <ul>
 *     <li>positions are <b>1-based</b></li>
 *     <li>array indexes are <b>0-based</b></li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * Object[] params = { 10L, true };
 * ParameterSource source = new ArrayParameterSource(params);
 *
 * // SQL: "select * from users where id = ? and active = ?"
 * }</pre>
 *
 * <p>
 * Named parameter access is explicitly unsupported and will result in
 * {@link UnsupportedOperationException}.
 *
 * @author jMouse
 */
public final class ArrayParameterSource implements ParameterSource {

    /**
     * Backing array holding positional parameter values.
     */
    private final Object[] arrayValues;

    /**
     * Creates a new {@code ArrayParameterSource}.
     *
     * @param arrayValues array of parameter values (1-based JDBC positions)
     */
    public ArrayParameterSource(Object[] arrayValues) {
        this.arrayValues = Objects.requireNonNull(arrayValues, "arrayValues");
    }

    /**
     * Checks whether a value exists for the given positional parameter.
     *
     * @param position 1-based JDBC parameter position
     * @return {@code true} if a value is present, {@code false} otherwise
     */
    @Override
    public boolean hasValue(int position) {
        int index = position - 1;
        return index >= 0 && index < arrayValues.length;
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
     * @throws ArrayIndexOutOfBoundsException if the position is out of range
     */
    @Override
    public Object getValue(int position) {
        return arrayValues[position - 1];
    }

    /**
     * Named parameters are not supported by this source.
     *
     * @param name parameter name
     * @throws UnsupportedOperationException always
     */
    @Override
    public Object getValue(String name) {
        throw new UnsupportedOperationException("Named parameters are not supported by ArrayParameterSource");
    }
}
