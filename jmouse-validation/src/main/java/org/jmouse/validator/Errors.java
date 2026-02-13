package org.jmouse.validator;

import org.jmouse.core.access.PropertyPath;

import java.util.List;
import java.util.function.Function;

/**
 * Collector for validation errors produced while validating an object. ✅
 *
 * <p>{@code Errors} is a Spring-like abstraction used by validators to record:</p>
 * <ul>
 *   <li><b>global errors</b> (object-level problems)</li>
 *   <li><b>field errors</b> (property-level problems)</li>
 * </ul>
 *
 * <p>Implementations typically keep the validated target (optional), expose current field values/types,
 * and provide error code resolution (message codes).</p>
 *
 * <p>Error reporting is done via:</p>
 * <ul>
 *   <li>{@link #reject(String, String, Object...)} for global errors</li>
 *   <li>{@link #rejectValue(String, String, String, Object...)} for field errors</li>
 * </ul>
 */
public interface Errors {

    /**
     * Logical name of the validated object (Spring-like: {@code "user"}, {@code "filter"}).
     *
     * @return object name used for reporting (never {@code null})
     */
    String getObjectName();

    /**
     * Validated target object.
     *
     * <p>May be {@code null} when validation happens before instantiation or when only
     * metadata-based validation is performed.</p>
     *
     * @return validated target (may be {@code null})
     */
    Object getTarget();

    /**
     * Register a global (object-level) error.
     *
     * @param errorCode stable error code key
     * @param defaultMessage fallback message (may be {@code null})
     * @param arguments optional arguments for message formatting/interpolation
     */
    void reject(String errorCode, String defaultMessage, Object... arguments);

    /**
     * Register a global error with no default message.
     *
     * @param errorCode stable error code key
     */
    default void reject(String errorCode) {
        reject(errorCode, null);
    }

    /**
     * Register a field (property-level) error.
     *
     * @param field field name or path (e.g. {@code "email"}, {@code "address.city"}, {@code "items[0].name"})
     * @param errorCode stable error code key
     * @param defaultMessage fallback message (may be {@code null})
     * @param arguments optional arguments for message formatting/interpolation
     */
    void rejectValue(String field, String errorCode, String defaultMessage, Object... arguments);

    /**
     * Register a field error with no default message.
     *
     * @param field field name/path
     * @param errorCode stable error code key
     */
    default void rejectValue(String field, String errorCode) {
        rejectValue(field, errorCode, null);
    }

    /**
     * Path-aware {@link #rejectValue(String, String, String, Object...)} overload.
     *
     * <p>If {@code path} is {@code null} or empty, this method falls back to {@link #reject(String, String, Object...)}
     * (global error).</p>
     *
     * @param path property path (may be {@code null})
     * @param errorCode stable error code key
     * @param defaultMessage fallback message (may be {@code null})
     * @param arguments optional arguments for message formatting/interpolation
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
     * @param path property path (may be {@code null})
     * @param errorCode stable error code key
     */
    default void rejectValue(PropertyPath path, String errorCode) {
        rejectValue(path, errorCode, null);
    }

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

    /**
     * Resolve message codes for the given error code (message resolution chain).
     *
     * @param code base error code
     * @return ordered array of message codes (never {@code null})
     */
    String[] getErrorCodes(String code);

    /**
     * All recorded field errors.
     *
     * @return list of field errors (never {@code null})
     */
    List<FieldError> getErrors();

    /**
     * First field error for the given {@code field}, or {@code null} when none exist.
     *
     * @param field field name/path
     * @return first field error or {@code null}
     */
    default FieldError getError(String field) {
        return getErrors(field).isEmpty() ? null : getErrors(field).getFirst();
    }

    /**
     * All field errors for the given {@code field}.
     *
     * @param field field name/path
     * @return list of matching field errors (never {@code null})
     */
    default List<FieldError> getErrors(String field) {
        return getErrors().stream().filter(e -> e.getField().equals(field)).toList();
    }

    /**
     * Whether any field errors are present.
     *
     * @return {@code true} if field errors exist
     */
    default boolean hasErrors() {
        return !getErrors().isEmpty();
    }

    /**
     * Whether any errors exist for the given field.
     *
     * @param field field name/path
     * @return {@code true} if at least one error exists for the field
     */
    default boolean hasErrors(String field) {
        return !getErrors(field).isEmpty();
    }

    /**
     * All recorded global (object-level) errors.
     *
     * @return list of global errors (never {@code null})
     */
    List<ObjectError> getGlobalErrors();

    /**
     * Whether any global (object-level) errors are present.
     *
     * @return {@code true} if global errors exist
     */
    default boolean hasGlobalErrors() {
        return !getGlobalErrors().isEmpty();
    }

    /**
     * Throw an exception produced by {@code function} when any errors are present.
     *
     * <p>The predicate is: {@code hasErrors() || hasGlobalErrors()}.</p>
     *
     * @param function exception factory (receives this {@link Errors})
     * @param <E> exception type
     * @throws E when any errors exist
     */
    default <E extends Throwable> void throwIfErrors(Function<Errors, E> function) throws E {
        if (hasErrors() || hasGlobalErrors()) {
            throw function.apply(this);
        }
    }
}
