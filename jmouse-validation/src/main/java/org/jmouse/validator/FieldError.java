package org.jmouse.validator;

/**
 * Represents a validation error associated with a specific field of an object.
 * <p>
 * This class extends {@link ObjectError} and includes additional information about
 * the field that caused the validation failure and the rejected value.
 * </p>
 *
 * @see ObjectError
 */
public class FieldError extends ObjectError {

    private final Object rejectedValue;
    private final String field;

    /**
     * Constructs a {@code FieldError} with details about the validation failure.
     *
     * @param objectName     the name of the object that contains the invalid field
     * @param field          the name of the field that failed validation
     * @param rejectedValue  the value that was rejected during validation
     * @param errorCodes     the message codes for localization
     * @param defaultMessage the default error message if no localized message is found
     * @param arguments      optional arguments for message formatting
     */
    public FieldError(String objectName, String field, Object rejectedValue, String[] errorCodes,
                      String defaultMessage, Object... arguments) {
        super(objectName, errorCodes, defaultMessage, arguments);

        this.rejectedValue = rejectedValue;
        this.field = field;
    }

    /**
     * Returns the value that was rejected during validation.
     *
     * @return the rejected value
     */
    public Object getRejectedValue() {
        return rejectedValue;
    }

    /**
     * Returns the name of the field that failed validation.
     *
     * @return the field name
     */
    public String getField() {
        return field;
    }
}
