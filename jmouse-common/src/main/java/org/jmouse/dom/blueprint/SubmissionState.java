package org.jmouse.dom.blueprint;

import java.util.Map;

/**
 * Submission state used by rendering hooks.
 *
 * <p>Contains submitted values and field-level errors.</p>
 */
public record SubmissionState(
        Map<String, Object> values,
        Map<String, String> errors
) {

    public static final String REQUEST_ATTRIBUTE = "submission";

    public SubmissionState {
        values = values == null ? Map.of() : Map.copyOf(values);
        errors = errors == null ? Map.of() : Map.copyOf(errors);
    }

    public static SubmissionState of(Map<String, Object> values, Map<String, String> errors) {
        return new SubmissionState(values, errors);
    }

    public boolean hasValue(String fieldName) {
        return values.containsKey(fieldName);
    }

    public Object value(String fieldName) {
        return values.get(fieldName);
    }

    public boolean hasError(String fieldName) {
        return errors.containsKey(fieldName);
    }

    public String errorMessage(String fieldName) {
        return errors.get(fieldName);
    }
}
