package org.jmouse.validator;

import org.jmouse.core.i18n.LocalizableMessage;

/**
 * Represents an error related to a specific object in the validation process.
 * <p>
 * This class extends {@link LocalizableMessage.Default} and associates an error message
 * with a specific object name, allowing better contextualization of validation errors.
 * </p>
 *
 * @see LocalizableMessage
 */
public class ObjectError extends LocalizableMessage.Default {

    private final String objectName;

    /**
     * Constructs an {@code ObjectError} with the specified object name, message key,
     * default message, and optional arguments.
     *
     * @param objectName        the name of the object related to this error
     * @param errorCodes        the message key used for localization
     * @param defaultMessage    the default message to use if the localized message is not found
     * @param arguments         optional arguments for message formatting
     */
    public ObjectError(String objectName, String[] errorCodes, String defaultMessage, Object... arguments) {
        super(errorCodes, defaultMessage, arguments);

        this.objectName = objectName;
    }

    /**
     * Returns the name of the object associated with this error.
     *
     * @return the object name
     */
    public String getObjectName() {
        return objectName;
    }
}
