package org.jmouse.meterializer;

import java.util.Map;

/**
 * Holds submitted form state during rendering.
 *
 * <p>Provides access to submitted field values and validation errors.</p>
 */
public record SubmissionState(
        Map<String, Object> values,
        Map<String, String> errors
) {

    /**
     * Request attribute key used to expose {@link SubmissionState}.
     */
    public static final String REQUEST_ATTRIBUTE = SubmissionState.class.getName() + ".request";

    /**
     * Canonical constructor with defensive copies.
     *
     * <p>{@code null} maps are replaced with empty ones.</p>
     */
    public SubmissionState {
        values = values == null ? Map.of() : Map.copyOf(values);
        errors = errors == null ? Map.of() : Map.copyOf(errors);
    }

    /**
     * Factory method for convenience.
     */
    public static SubmissionState of(Map<String, Object> values, Map<String, String> errors) {
        return new SubmissionState(values, errors);
    }

    /**
     * Returns {@code true} if a value for the field exists.
     */
    public boolean hasValue(String fieldName) {
        return values.containsKey(fieldName);
    }

    /**
     * Returns submitted value for the field or {@code null}.
     */
    public Object value(String fieldName) {
        return values.get(fieldName);
    }

    /**
     * Returns {@code true} if the field has a validation error.
     */
    public boolean hasError(String fieldName) {
        return errors.containsKey(fieldName);
    }

    /**
     * Returns validation error message for the field or {@code null}.
     */
    public String errorMessage(String fieldName) {
        return errors.get(fieldName);
    }
}