package org.jmouse.dom.blueprint;

import org.jmouse.core.Verify;

import java.util.List;

/**
 * A predicate used by blueprint nodes.
 */
public sealed interface BlueprintPredicate
        permits BlueprintPredicate.BooleanValuePredicate,
                BlueprintPredicate.PresentPredicate,
                BlueprintPredicate.EqualityPredicate,
                BlueprintPredicate.NotPredicate,
                BlueprintPredicate.AllPredicate,
                BlueprintPredicate.AnyPredicate,
                BlueprintPredicate.ContainsPredicate {

    /**
     * Evaluates a boolean-like value resolved from {@link BlueprintValue}.
     *
     * <p>Rules are defined by the predicate evaluator (Boolean, number, string, null).</p>
     */
    record BooleanValuePredicate(BlueprintValue value) implements BlueprintPredicate {
        public BooleanValuePredicate {
            Verify.nonNull(value, "value");
        }
    }

    /**
     * Returns true if a value exists and is not considered empty.
     *
     * <p>Typical checks: not null; non-empty string; non-empty collection; non-empty map.</p>
     */
    record PresentPredicate(BlueprintValue value) implements BlueprintPredicate {
        public PresentPredicate {
            Verify.nonNull(value, "value");
        }
    }

    /**
     * Compares two resolved values for equality.
     *
     * <p>Comparison rules are defined by the predicate evaluator.
     * Recommended baseline: Objects.equals(left, right).</p>
     */
    record EqualityPredicate(BlueprintValue left, BlueprintValue right) implements BlueprintPredicate {
        public EqualityPredicate {
            Verify.nonNull(left, "left");
            Verify.nonNull(right, "right");
        }
    }

    /**
     * Logical negation.
     */
    record NotPredicate(BlueprintPredicate inner) implements BlueprintPredicate {
        public NotPredicate {
            Verify.nonNull(inner, "inner");
        }
    }

    /**
     * Logical AND over all predicates.
     */
    record AllPredicate(List<BlueprintPredicate> predicates) implements BlueprintPredicate {
        public AllPredicate {
            predicates = List.copyOf(predicates);
        }
    }

    /**
     * Logical OR over predicates.
     */
    record AnyPredicate(List<BlueprintPredicate> predicates) implements BlueprintPredicate {
        public AnyPredicate {
            predicates = List.copyOf(predicates);
        }
    }

    /**
     * Contains predicates.
     */
    record ContainsPredicate(BlueprintValue collection, BlueprintValue value) implements BlueprintPredicate {}

}

