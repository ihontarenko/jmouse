package org.jmouse.meterializer;

import org.jmouse.core.Verify;

import java.util.List;

import static org.jmouse.meterializer.ValueExpression.path;

/**
 * Declarative predicate used by blueprint nodes to control rendering flow. 🔎
 *
 * <p>
 * A {@code TemplatePredicate} forms a small logical expression tree that is
 * evaluated at runtime by {@code PredicateEvaluator}. Each predicate operates
 * on {@link ValueExpression} instances that are resolved against the current
 * {@link RenderingExecution}.
 * </p>
 *
 * <h3>Supported predicate types</h3>
 * <ul>
 *     <li>{@link BooleanValue} — truthy evaluation</li>
 *     <li>{@link Present} — presence / non-empty check</li>
 *     <li>{@link Equality} — value comparison</li>
 *     <li>{@link Not} — logical negation</li>
 *     <li>{@link All} — logical AND</li>
 *     <li>{@link Any} — logical OR</li>
 *     <li>{@link Contains} — membership / substring check</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * // user.role == "ADMIN" && present(user.name)
 * TemplatePredicate predicate = TemplatePredicate.all(
 *     TemplatePredicate.same(
 *         ValueExpression.path("user.role"),
 *         ValueExpression.constant("ADMIN")
 *     ),
 *     TemplatePredicate.present(
 *         ValueExpression.path("user.name")
 *     )
 * );
 * }</pre>
 *
 * <p>
 * Predicates are immutable and composable. They describe intent only;
 * evaluation semantics are defined by the runtime evaluator.
 * </p>
 */
public sealed interface TemplatePredicate
        permits TemplatePredicate.BooleanValue,
        TemplatePredicate.Present,
        TemplatePredicate.Equality,
        TemplatePredicate.Not,
        TemplatePredicate.All,
        TemplatePredicate.Any,
        TemplatePredicate.Contains {

    /**
     * Creates a truthy predicate.
     *
     * @param value expression to evaluate
     * @return boolean predicate
     */
    static TemplatePredicate truthy(ValueExpression value) {
        return new TemplatePredicate.BooleanValue(value);
    }

    /**
     * Creates a presence predicate.
     *
     * @param value expression to check
     * @return presence predicate
     */
    static TemplatePredicate present(ValueExpression value) {
        return new TemplatePredicate.Present(value);
    }

    /**
     * Creates an equality predicate.
     *
     * @param left  left expression
     * @param right right expression
     * @return equality predicate
     */
    static TemplatePredicate same(ValueExpression left, ValueExpression right) {
        return new TemplatePredicate.Equality(left, right);
    }

    /**
     * Creates a negation predicate.
     *
     * @param inner inner predicate
     * @return negated predicate
     */
    static TemplatePredicate not(TemplatePredicate inner) {
        return new TemplatePredicate.Not(inner);
    }

    /**
     * Creates a logical AND predicate.
     *
     * @param predicates nested predicates
     * @return conjunction predicate
     */
    static TemplatePredicate all(TemplatePredicate... predicates) {
        return new TemplatePredicate.All(List.of(predicates));
    }

    /**
     * Creates a logical OR predicate.
     *
     * @param predicates nested predicates
     * @return disjunction predicate
     */
    static TemplatePredicate any(TemplatePredicate... predicates) {
        return new TemplatePredicate.Any(List.of(predicates));
    }

    /**
     * Creates a containment predicate.
     *
     * @param collection expression representing collection / string / map
     * @param value      searched value
     * @return contains predicate
     */
    static TemplatePredicate contains(ValueExpression collection, ValueExpression value) {
        return new TemplatePredicate.Contains(collection, value);
    }

    /**
     * Shortcut for {@link #truthy(ValueExpression)} using a path expression.
     *
     * @param path data path (e.g. {@code "user.enabled"})
     * @return boolean predicate
     */
    static TemplatePredicate pathBoolean(String path) {
        return new TemplatePredicate.BooleanValue(
                path(path)
        );
    }

    /**
     * Evaluates a boolean-like value resolved from {@link ValueExpression}.
     *
     * <p>
     * Coercion rules (null/boolean/number/string) are defined by the
     * {@code PredicateEvaluator}.
     * </p>
     */
    record BooleanValue(ValueExpression value) implements TemplatePredicate {
        public BooleanValue {
            Verify.nonNull(value, "value");
        }
    }

    /**
     * Returns {@code true} if a resolved value exists and is not considered empty.
     *
     * <p>Typical checks include:</p>
     * <ul>
     *     <li>not {@code null}</li>
     *     <li>non-empty string</li>
     *     <li>non-empty collection</li>
     *     <li>non-empty map</li>
     * </ul>
     */
    record Present(ValueExpression value) implements TemplatePredicate {
        public Present {
            Verify.nonNull(value, "value");
        }
    }

    /**
     * Compares two resolved expressions for equality.
     *
     * <p>
     * The evaluator defines comparison semantics.
     * A typical baseline implementation uses {@code Objects.equals}.
     * </p>
     */
    record Equality(ValueExpression left, ValueExpression right) implements TemplatePredicate {
        public Equality {
            Verify.nonNull(left, "left");
            Verify.nonNull(right, "right");
        }
    }

    /**
     * Logical negation of a predicate.
     */
    record Not(TemplatePredicate inner) implements TemplatePredicate {
        public Not {
            Verify.nonNull(inner, "inner");
        }
    }

    /**
     * Logical AND over all nested predicates.
     *
     * <p>Evaluation typically short-circuits on first {@code false}.</p>
     */
    record All(List<TemplatePredicate> predicates) implements TemplatePredicate {
        public All {
            predicates = List.copyOf(predicates);
        }
    }

    /**
     * Logical OR over nested predicates.
     *
     * <p>Evaluation typically short-circuits on first {@code true}.</p>
     */
    record Any(List<TemplatePredicate> predicates) implements TemplatePredicate {
        public Any {
            predicates = List.copyOf(predicates);
        }
    }

    /**
     * Containment predicate.
     *
     * <p>
     * Supported runtime targets typically include:
     * collections, maps (key lookup), strings (substring), arrays.
     * </p>
     */
    record Contains(ValueExpression collection, ValueExpression value) implements TemplatePredicate {
    }

}

