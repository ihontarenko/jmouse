package org.jmouse.meterializer;

import org.jmouse.core.Verify;

import java.util.List;

import static org.jmouse.meterializer.ValueExpression.path;

/**
 * A predicate used by blueprint nodes.
 */
public sealed interface TemplatePredicate
        permits TemplatePredicate.BooleanValue,
                TemplatePredicate.Present,
                TemplatePredicate.Equality,
                TemplatePredicate.Not,
                TemplatePredicate.All,
                TemplatePredicate.Any,
                TemplatePredicate.Contains {

    static TemplatePredicate truthy(ValueExpression value) {
        return new TemplatePredicate.BooleanValue(value);
    }

    static TemplatePredicate present(ValueExpression value) {
        return new TemplatePredicate.Present(value);
    }

    static TemplatePredicate same(ValueExpression left, ValueExpression right) {
        return new TemplatePredicate.Equality(left, right);
    }

    static TemplatePredicate not(TemplatePredicate inner) {
        return new TemplatePredicate.Not(inner);
    }

    static TemplatePredicate all(TemplatePredicate... predicates) {
        return new TemplatePredicate.All(List.of(predicates));
    }

    static TemplatePredicate any(TemplatePredicate... predicates) {
        return new TemplatePredicate.Any(List.of(predicates));
    }

    static TemplatePredicate contains(ValueExpression collection, ValueExpression value) {
        return new TemplatePredicate.Contains(collection, value);
    }

    static TemplatePredicate pathBoolean(String path) {
        return new TemplatePredicate.BooleanValue(
                path(path)
        );
    }

    /**
     * Evaluates a boolean-like value resolved from {@link ValueExpression}.
     *
     * <p>Rules are defined by the predicate evaluator (Boolean, number, string, null).</p>
     */
    record BooleanValue(ValueExpression value) implements TemplatePredicate {
        public BooleanValue {
            Verify.nonNull(value, "value");
        }
    }

    /**
     * Returns true if a value exists and is not considered empty.
     *
     * <p>Typical checks: not null; non-empty string; non-empty collection; non-empty map.</p>
     */
    record Present(ValueExpression value) implements TemplatePredicate {
        public Present {
            Verify.nonNull(value, "value");
        }
    }

    /**
     * Compares two resolved values for equality.
     *
     * <p>Comparison rules are defined by the predicate evaluator.
     * Recommended baseline: Objects.equals(left, right).</p>
     */
    record Equality(ValueExpression left, ValueExpression right) implements TemplatePredicate {
        public Equality {
            Verify.nonNull(left, "left");
            Verify.nonNull(right, "right");
        }
    }

    /**
     * Logical negation.
     */
    record Not(TemplatePredicate inner) implements TemplatePredicate {
        public Not {
            Verify.nonNull(inner, "inner");
        }
    }

    /**
     * Logical AND over all predicates.
     */
    record All(List<TemplatePredicate> predicates) implements TemplatePredicate {
        public All {
            predicates = List.copyOf(predicates);
        }
    }

    /**
     * Logical OR over predicates.
     */
    record Any(List<TemplatePredicate> predicates) implements TemplatePredicate {
        public Any {
            predicates = List.copyOf(predicates);
        }
    }

    /**
     * Contains predicates.
     */
    record Contains(ValueExpression collection, ValueExpression value) implements TemplatePredicate {
    }

}

