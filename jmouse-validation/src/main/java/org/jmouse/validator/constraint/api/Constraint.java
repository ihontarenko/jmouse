package org.jmouse.validator.constraint.api;

/**
 * Core validation contract representing a single constraint rule. 🧠
 *
 * <p>
 * A {@code Constraint} defines:
 * </p>
 * <ul>
 *     <li>a stable {@link #code()} used for error identification,</li>
 *     <li>an optional default {@link #message()},</li>
 *     <li>a {@link ConstraintExecutor} responsible for evaluation logic.</li>
 * </ul>
 *
 * <p>
 * Constraints are typically registered inside a {@code ConstraintSchema}
 * and executed by a validation processor.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * public final class RequiredConstraint implements Constraint {
 *
 *     @Override
 *     public String code() {
 *         return "required";
 *     }
 *
 *     @Override
 *     public ConstraintExecutor<RequiredConstraint> executor() {
 *         return (value, constraint) -> value != null;
 *     }
 * }
 * }</pre>
 *
 * <p>
 * ⚠ Implementations should keep {@link #code()} stable, as it is used
 * for error codes, DSL registration, and message resolution.
 * </p>
 */
public interface Constraint {

    /**
     * Stable identifier used as error code suffix, registry key, and DSL name.
     *
     * <p>Examples: {@code "minMax"}, {@code "oneOf"}, {@code "required"}.</p>
     *
     * @return unique constraint code
     */
    String code();

    /**
     * Default message used when constraint fails.
     *
     * <p>
     * May return {@code null} if message resolution is handled externally
     * (e.g. via message codes or localization).
     * </p>
     *
     * @return default message or {@code null}
     */
    default String message() {
        return null;
    }

    /**
     * Provides the executor responsible for evaluating this constraint.
     *
     * @return constraint executor
     */
    ConstraintExecutor<? extends Constraint> executor();

    /**
     * Executes this constraint against the provided value.
     *
     * <p>
     * Delegates to the associated {@link ConstraintExecutor}.
     * </p>
     *
     * @param value value to validate
     * @return {@code true} if validation passes, otherwise {@code false}
     */
    default boolean execute(Object value) {
        @SuppressWarnings("unchecked")
        ConstraintExecutor<Constraint> executor =
                (ConstraintExecutor<Constraint>) executor();
        return executor.test(value, this);
    }

    /**
     * Returns message arguments used for message interpolation.
     *
     * @return constraint arguments (never {@code null})
     */
    default Object[] arguments() {
        return new Object[0];
    }
}