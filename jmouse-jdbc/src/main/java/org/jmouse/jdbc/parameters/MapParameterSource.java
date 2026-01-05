package org.jmouse.jdbc.parameters;

import org.jmouse.core.Contract;

import java.util.Map;

/**
 * {@link ParameterSource} implementation backed by a {@link Map} of named values.
 * <p>
 * {@code MapParameterSource} provides values for <b>named</b> SQL parameters
 * (e.g. {@code :id}, {@code :name}) and does not support positional parameters.
 *
 * <p>
 * Parameter names are matched exactly as provided in the SQL plan.
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * Map<String, Object> params = Map.of(
 *     "id", 10L,
 *     "active", true
 * );
 *
 * ParameterSource source = new MapParameterSource(params);
 *
 * // SQL: "select * from users where id = :id and active = :active"
 * }</pre>
 *
 * <p>
 * Positional parameter access is explicitly unsupported and will result in
 * {@link UnsupportedOperationException}.
 *
 * @author jMouse
 */
public final class MapParameterSource implements ParameterSource {

    /**
     * Backing map containing named parameter values.
     */
    private final Map<String, ?> values;

    /**
     * Creates a new {@code MapParameterSource}.
     *
     * @param values map of parameter names to values
     */
    public MapParameterSource(Map<String, ?> values) {
        this.values = Contract.nonNull(values, "values");
    }

    /**
     * Always returns {@code false} as positional parameters are not supported.
     *
     * @param position JDBC parameter position
     * @return {@code false}
     */
    @Override
    public boolean hasValue(int position) {
        return false;
    }

    /**
     * Checks whether a value exists for the given named parameter.
     *
     * @param name parameter name
     * @return {@code true} if the parameter is present
     */
    @Override
    public boolean hasValue(String name) {
        return values.containsKey(name);
    }

    /**
     * Positional parameters are not supported by this source.
     *
     * @param position JDBC parameter position
     * @throws UnsupportedOperationException always
     */
    @Override
    public Object getValue(int position) {
        throw new UnsupportedOperationException("Positional parameters are not supported by MapParameterSource");
    }

    /**
     * Returns the value associated with the given named parameter.
     *
     * @param name parameter name
     * @return parameter value (may be {@code null})
     */
    @Override
    public Object getValue(String name) {
        return values.get(name);
    }
}
