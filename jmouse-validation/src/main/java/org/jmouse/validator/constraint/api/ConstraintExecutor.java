package org.jmouse.validator.constraint.api;

/**
 * Functional contract responsible for evaluating a {@link Constraint}. ⚙️
 *
 * <p>
 * A {@code ConstraintExecutor} contains the actual validation logic
 * for a specific constraint type.
 * </p>
 *
 * <p>
 * It receives:
 * </p>
 * <ul>
 *     <li>the current field value (possibly {@code null}),</li>
 *     <li>the typed constraint configuration instance.</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * public final class MinConstraint implements Constraint {
 *
 *     private final int min;
 *
 *     public MinConstraint(int min) {
 *         this.min = min;
 *     }
 *
 *     public int min() {
 *         return min;
 *     }
 *
 *     @Override
 *     public String code() {
 *         return "min";
 *     }
 *
 *     @Override
 *     public ConstraintExecutor<MinConstraint> executor() {
 *         return (value, constraint) -> {
 *             if (value == null) return true;
 *             if (!(value instanceof Number number)) return false;
 *             return number.intValue() >= constraint.min();
 *         };
 *     }
 * }
 * }</pre>
 *
 * @param <T> concrete constraint type
 */
@FunctionalInterface
public interface ConstraintExecutor<T extends Constraint> {

    /**
     * Evaluates the constraint against the given value.
     *
     * @param value       current field value (may be {@code null})
     * @param constraint  typed constraint configuration
     * @return {@code true} if valid, {@code false} otherwise
     */
    boolean test(Object value, T constraint);
}