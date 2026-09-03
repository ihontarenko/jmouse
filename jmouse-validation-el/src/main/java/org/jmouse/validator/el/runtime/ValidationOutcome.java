package org.jmouse.validator.el.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a document had to say about one record. 📋
 *
 * <h2>⚠️ "No errors" and "not asked" are different answers</h2>
 *
 * <p>A field under a guard that did not hold was never checked, and that is not the same as its having
 * passed. {@link #skipped()} names those fields, so a caller can tell the two apart — a screen that
 * renders a green tick against a field nobody examined is lying in the quietest possible way.</p>
 *
 * <p>⚠️ {@link #gated()} says the gate refused and <strong>nothing else ran</strong>. Without it, one
 * error from a gate is indistinguishable from one error out of thirty checks, and the difference is
 * the whole point of having a gate.</p>
 *
 * @param errors  every complaint, in the order the document raises them
 * @param skipped fields the document mentions that this record's shape did not reach
 * @param gated   whether the gate refused, leaving the body unevaluated
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record ValidationOutcome(List<ValidationError> errors, List<String> skipped, boolean gated) {

    public ValidationOutcome {
        errors = List.copyOf(errors);
        skipped = List.copyOf(skipped);
    }

    /** @return whether the record is acceptable */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * The complaints gathered by field, in the order they were raised.
     *
     * <p>An invariant belongs to no field, so it appears under {@code null} — which is deliberate
     * rather than convenient: a caller has to decide where to show it, and a key that quietly named
     * one of the fields it mentions would decide for them, wrongly half the time.</p>
     *
     * @return the complaints, keyed by field
     */
    public Map<String, List<String>> byField() {
        Map<String, List<String>> gathered = new LinkedHashMap<>();

        for (ValidationError error : errors) {
            gathered.computeIfAbsent(error.field(), field -> new java.util.ArrayList<>())
                    .add(error.message());
        }

        return gathered;
    }

    @Override
    public String toString() {
        if (isValid()) {
            return "valid" + (skipped.isEmpty() ? "" : " (not asked: " + String.join(", ", skipped) + ")");
        }

        return (gated ? "gated: " : "") + errors;
    }
}
