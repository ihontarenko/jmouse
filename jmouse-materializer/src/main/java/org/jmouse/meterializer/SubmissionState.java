package org.jmouse.meterializer;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.core.access.ValueNavigator;

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

    private static final ValueNavigator  NAVIGATOR = ValueNavigator.defaultNavigator();
    private static final AccessorWrapper WRAPPER   = new ObjectAccessorWrapper();

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
        if (values.containsKey(fieldName)) {
            return true;
        }
        return NAVIGATOR.navigate(WRAPPER.wrap(values), fieldName) != null;
    }

    /**
     * Returns submitted value for the field or {@code null}.
     */
    public Object getValue(String fieldName) {
        Object value = values.get(fieldName);

        if (value == null) {
            value = NAVIGATOR.navigate(WRAPPER.wrap(values), fieldName);
        }

        return value;
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