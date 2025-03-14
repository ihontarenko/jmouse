package org.jmouse.validator;

import java.util.List;
import java.util.function.Function;

/**
 * Represents an interface for handling validation errors.
 * <p>
 * This interface provides methods for rejecting errors related to specific fields
 * or general object-level errors, retrieving errors, and checking their presence.
 * </p>
 *
 * @see FieldError
 * @see ObjectError
 */
public interface Errors {

    /**
     * Registers a parser error with the given error code, default message, and optional arguments.
     *
     * @param errorCode      the error code identifying the validation issue
     * @param defaultMessage the default message if no localized message is found
     * @param arguments      optional arguments for message formatting
     */
    void reject(String errorCode, String defaultMessage, Object... arguments);

    /**
     * Registers a parser error with the given error code, using {@code null} as the default message.
     *
     * @param errorCode the error code identifying the validation issue
     */
    default void reject(String errorCode) {
        reject(errorCode, null);
    }

    /**
     * Registers a field-specific error with the given field name, error code, default message,
     * and optional arguments.
     *
     * @param field          the name of the field that has the validation error
     * @param errorCode      the error code identifying the validation issue
     * @param defaultMessage the default message if no localized message is found
     * @param arguments      optional arguments for message formatting
     */
    void rejectValue(String field, String errorCode, String defaultMessage, Object... arguments);

    /**
     * Registers a field-specific error with the given field name and error code, using {@code null}
     * as the default message.
     *
     * @param field     the name of the field that has the validation error
     * @param errorCode the error code identifying the validation issue
     */
    default void rejectValue(String field, String errorCode) {
        rejectValue(field, errorCode, null);
    }

    /**
     * Retrieves the current value of a specific field in the validated object.
     * <p>
     * This method allows access to the actual value being validated, which can be useful
     * for debugging or advanced validation logic.
     * </p>
     *
     * @param field the name of the field
     * @return the value of the field, or {@code null} if unavailable
     */
    Object getFieldValue(String field);

    /**
     * Retrieves the type of a specific field in the validated object.
     * <p>
     * This method provides insight into the expected data type of a field, which can be useful
     * for dynamic validation rules.
     * </p>
     *
     * @param field the name of the field
     * @return the {@link Class} representing the field type, or {@code null} if unknown
     */
    Class<?> getFieldType(String field);

    /**
     * Resolves error codes based on a given error code.
     * <p>
     * This method allows for error code expansion, which can be used in hierarchical
     * validation error handling. Implementations may append additional codes
     * to provide more contextual validation messages.
     * </p>
     *
     * @param code the base error code
     * @return an array of error codes, where the first one is typically the most specific
     */
    String[] getErrorCodes(String code);

    /**
     * Returns a list of all field errors associated with this validation instance.
     *
     * @return a list of {@link FieldError} instances
     */
    List<FieldError> getErrors();

    /**
     * Retrieves the first error associated with the specified field.
     *
     * @param field the name of the field
     * @return the first {@link FieldError} for the field, or {@code null} if none exist
     */
    default FieldError getError(String field) {
        return getErrors(field).isEmpty() ? null : getErrors(field).getFirst();
    }

    /**
     * Retrieves all errors associated with the specified field.
     *
     * @param field the name of the field
     * @return a list of {@link FieldError} instances for the specified field
     */
    default List<FieldError> getErrors(String field) {
        return getErrors().stream().filter(e -> e.getField().equals(field)).toList();
    }

    /**
     * Checks if there are any validation errors.
     *
     * @return {@code true} if errors are present, {@code false} otherwise
     */
    default boolean hasErrors() {
        return !getErrors().isEmpty();
    }

    /**
     * Checks if there are any errors associated with the specified field.
     *
     * @param field the name of the field
     * @return {@code true} if errors exist for the field, {@code false} otherwise
     */
    default boolean hasErrors(String field) {
        return !getErrors(field).isEmpty();
    }

    /**
     * Returns a list of all parser (non-field-specific) errors.
     *
     * @return a list of {@link ObjectError} instances
     */
    List<ObjectError> getGlobalErrors();

    /**
     * Checks if there are any parser (non-field-specific) validation errors.
     *
     * @return {@code true} if parser errors exist, {@code false} otherwise
     */
    default boolean hasGlobalErrors() {
        return !getGlobalErrors().isEmpty();
    }

    /**
     * Throws an exception generated by the provided function if validation errors exist.
     * <p>
     * This method allows custom exception handling based on validation results.
     * </p>
     *
     * @param function a function that converts {@code Errors} into an exception
     * @param <E>      the type of exception to be thrown
     * @throws E if validation errors exist
     */
    default <E extends Throwable> void throwIfErrors(Function<Errors, E> function) throws E {
        if (hasErrors()) {
            throw function.apply(this);
        }
    }
}
