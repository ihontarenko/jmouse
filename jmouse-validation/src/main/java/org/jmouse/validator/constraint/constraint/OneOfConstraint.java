package org.jmouse.validator.constraint.constraint;

import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.api.ConstraintExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * Constraint ensuring that a value belongs to a predefined set of allowed values. 🎯
 *
 * <p>
 * Values are compared using their {@link String} representation to provide
 * stable cross-type comparison (e.g. {@code 1}, {@code "1"}, enums, etc.).
 * </p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *     <li>{@code null} values are considered valid (combine with {@code required} if needed).</li>
 *     <li>If {@code allowed} list is {@code null} or empty, validation always passes.</li>
 *     <li>Comparison is performed using {@code String.valueOf(value)}.</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * OneOfConstraint constraint = new OneOfConstraint();
 * constraint.setAllowed(List.of("ADMIN", "USER", "GUEST"));
 *
 * boolean ok = constraint.execute("ADMIN");  // true
 * boolean fail = constraint.execute("ROOT"); // false
 * }</pre>
 */
public final class OneOfConstraint implements Constraint {

    /**
     * Allowed values stored as strings for stable comparison.
     */
    private List<String> allowed;

    private String message;

    /**
     * Returns stable constraint code.
     *
     * @return {@code "one_of"}
     */
    @Override
    public String code() {
        return "one_of";
    }

    /**
     * Returns custom message override (may be {@code null}).
     */
    @Override
    public String message() {
        return getMessage();
    }

    /**
     * @return list of allowed values (may be {@code null})
     */
    public List<String> getAllowed() {
        return allowed;
    }

    /**
     * Sets allowed values.
     *
     * <p>
     * Internally copies the list to avoid external mutation.
     * If {@code null} is provided, an empty list is stored.
     * </p>
     *
     * @param allowed allowed values
     */
    public void setAllowed(List<String> allowed) {
        this.allowed = (allowed == null)
                ? new ArrayList<>()
                : new ArrayList<>(allowed);
    }

    /**
     * @return custom validation message (may be {@code null})
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets custom validation message.
     *
     * @param message message override
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns shared executor instance.
     */
    @Override
    public ConstraintExecutor<OneOfConstraint> executor() {
        return Executor.INSTANCE;
    }

    /**
     * Executor implementation for {@link OneOfConstraint}.
     */
    static final class Executor implements ConstraintExecutor<OneOfConstraint> {

        static final Executor INSTANCE = new Executor();

        private Executor() {}

        @Override
        public boolean test(Object value, OneOfConstraint constraint) {
            if (value == null) {
                return true;
            }

            if (constraint.allowed == null || constraint.allowed.isEmpty()) {
                return true;
            }

            String normalized = String.valueOf(value);
            return constraint.allowed.contains(normalized);
        }
    }
}