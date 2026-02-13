package org.jmouse.validator;

import org.jmouse.core.access.PropertyPath;

import java.util.List;

/**
 * Collector for validation errors with support for nested property paths. ✅
 *
 * <p>{@code Errors} is a Spring-like abstraction used by validators to record:</p>
 * <ul>
 *   <li><b>global errors</b> (object-level problems)</li>
 *   <li><b>field errors</b> (property-level problems)</li>
 * </ul>
 *
 * <p>In addition to basic error reporting, this interface supports a <b>nested path</b>
 * concept. The current nested path acts as a prefix for field names passed to
 * {@link #rejectValue(String, String, String, Object...)}.</p>
 */
public interface Errors {

    /**
     * Logical name of the validated object (e.g. {@code "user"}, {@code "filter"}).
     *
     * @return object name used for reporting (never {@code null})
     */
    String getObjectName();

    /**
     * Validated target object (may be {@code null}).
     *
     * @return target object
     */
    Object getTarget();

    /**
     * Current nested path prefix.
     *
     * <p>The nested path is used as a prefix for field errors recorded via
     * {@link #rejectValue(String, String, String, Object...)}.</p>
     *
     * @return current nested path (never {@code null}, may be {@link PropertyPath#empty()})
     */
    PropertyPath getNestedPath();

    /**
     * Push a nested path segment.
     *
     * <p>After this call, {@code rejectValue("field", ...)} is interpreted as
     * {@code rejectValue("segment.field", ...)} (path concatenation rules depend on {@link PropertyPath}).</p>
     *
     * @param segment nested path segment to append
     */
    void pushPath(String segment);

    /**
     * Push a nested path segment.
     *
     * @param segment nested path segment to append
     */
    void pushPath(PropertyPath segment);

    /**
     * Pop the last nested path segment.
     *
     * <p>If the nested path is already empty, implementations may either keep it empty or throw,
     * depending on policy.</p>
     */
    void popPath();

    /**
     * Register a global (object-level) error.
     *
     * @param errorCode stable error code
     * @param defaultMessage fallback message (may be {@code null})
     * @param arguments optional message arguments
     */
    void reject(String errorCode, String defaultMessage, Object... arguments);

    /**
     * Register a global error with no default message.
     *
     * @param errorCode stable error code
     */
    default void reject(String errorCode) {
        reject(errorCode, null);
    }

    /**
     * Register a field (property-level) error.
     *
     * <p>Implementations should apply {@link #getNestedPath()} as a prefix to {@code field}
     * when the nested path is not empty.</p>
     *
     * @param field field name/path (relative to current nested path)
     * @param errorCode stable error code
     * @param defaultMessage fallback message (may be {@code null})
     * @param arguments optional message arguments
     */
    void rejectValue(String field, String errorCode, String defaultMessage, Object... arguments);

    /**
     * Register a field error with no default message.
     *
     * @param field field name/path
     * @param errorCode stable error code
     */
    default void rejectValue(String field, String errorCode) {
        rejectValue(field, errorCode, null);
    }

    /**
     * Path-aware overload for recording a field error.
     *
     * <p>If {@code path} is {@code null} or empty, falls back to {@link #reject(String, String, Object...)}.</p>
     *
     * @param path absolute or relative path
     * @param errorCode stable error code
     * @param defaultMessage fallback message (may be {@code null})
     * @param arguments optional message arguments
     */
    default void rejectValue(PropertyPath path, String errorCode, String defaultMessage, Object... arguments) {
        if (path == null || path.isEmpty()) {
            reject(errorCode, defaultMessage, arguments);
            return;
        }
        rejectValue(path.path(), errorCode, defaultMessage, arguments);
    }

    /**
     * Path-aware field reject with no default message.
     *
     * @param path absolute or relative path
     * @param errorCode stable error code
     */
    default void rejectValue(PropertyPath path, String errorCode) {
        rejectValue(path, errorCode, null);
    }

    // ===== field metadata =====

    /**
     * Read current field value from the underlying target/binding model.
     *
     * @param field field name/path
     * @return current field value (may be {@code null})
     */
    Object getFieldValue(String field);

    /**
     * Read the declared/known field type.
     *
     * @param field field name/path
     * @return field type (may be {@code null} if unknown)
     */
    Class<?> getFieldType(String field);

    // ===== accessors =====

    /**
     * All recorded field errors.
     *
     * @return list of field errors (never {@code null})
     */
    List<FieldError> getErrors();

    /**
     * All recorded global (object-level) errors.
     *
     * @return list of global errors (never {@code null})
     */
    List<ObjectError> getGlobalErrors();

    /**
     * @return {@code true} if any field errors exist
     */
    default boolean hasErrors() {
        return !getErrors().isEmpty();
    }

    /**
     * @return {@code true} if any global errors exist
     */
    default boolean hasGlobalErrors() {
        return !getGlobalErrors().isEmpty();
    }
}
