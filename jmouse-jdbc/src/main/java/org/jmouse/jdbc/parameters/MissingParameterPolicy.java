package org.jmouse.jdbc.parameters;

/**
 * Policy defining how missing SQL parameters should be handled during
 * prepared-statement binding.
 * <p>
 * {@code MissingParameterPolicy} is used by parameter binders (e.g.
 * {@code SQLPlanPreparedStatementBinder}) to decide what to do when a
 * named or positional parameter is referenced in SQL but not provided
 * by the {@link org.jmouse.jdbc.parameters.ParameterSource}.
 *
 * <h3>Available policies</h3>
 * <ul>
 *     <li>{@link #FAIL_FAST} – immediately throw an exception when a parameter is missing</li>
 *     <li>{@link #BIND_NULL} – bind {@code null} to the corresponding JDBC parameter</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * MissingParameterPolicy policy = MissingParameterPolicy.FAIL_FAST;
 * }</pre>
 *
 * <p>
 * Choosing the appropriate policy depends on whether missing parameters
 * are considered a programming error or a valid runtime condition.
 *
 * @author jMouse
 */
public enum MissingParameterPolicy {

    /**
     * Fail immediately if a required parameter is missing.
     * <p>
     * This policy is useful for strict validation and early error detection.
     */
    FAIL_FAST,

    /**
     * Bind {@code null} for missing parameters.
     * <p>
     * This policy allows optional parameters and defers null-handling
     * to the database.
     */
    BIND_NULL
}
