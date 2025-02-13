package org.jmouse.core.i18n;

import org.jmouse.util.helper.Arrays;

import java.text.MessageFormat;

/**
 * Represents a localizable message that can be resolved using a message source.
 * <p>
 * This interface defines the structure of a message that includes a message key,
 * optional arguments for formatting, and a default fallback message.
 * Implementations of this interface allow integration with localization frameworks.
 * </p>
 *
 * @see MessageSource
 */
public interface LocalizableMessage {

    /**
     * Creates a new {@code LocalizableMessage} instance with a specified message key,
     * default message, and optional arguments.
     *
     * @param code           the message key for localization
     * @param defaultMessage the fallback message if the localized message is not found
     * @param arguments      optional arguments for message formatting
     * @return a new {@link LocalizableMessage} instance
     */
    static LocalizableMessage of(String code, String defaultMessage, Object... arguments) {
        return new LocalizableMessage.Default(new String[]{code}, defaultMessage, arguments);
    }

    /**
     * Creates a new {@code LocalizableMessage} instance with a specified message code
     * and optional arguments, using {@code null} as the default message.
     *
     * @param code       the message code for localization
     * @param arguments optional arguments for message formatting
     * @return a new {@link LocalizableMessage} instance
     */
    static LocalizableMessage of(String code, Object... arguments) {
        return of(code, null, arguments);
    }

    /**
     * Returns the message codes used for localization.
     * <p>
     * The key is typically used to retrieve the localized message from a message source.
     * </p>
     *
     * @return the message key
     */
    String[] getCodes();

    /**
     * Returns the message key used for localization.
     * <p>
     * The key is typically used to retrieve the localized message from a message source.
     * </p>
     *
     * @return the message key
     */
    default String getCode() {
        return Arrays.get(getCodes(), 0, null);
    }

    /**
     * Returns the arguments used for formatting the localized message.
     * <p>
     * These arguments are applied to the resolved message using a formatting mechanism
     * such as {@link java.text.MessageFormat}.
     * </p>
     *
     * @return an array of arguments for message formatting, or an empty array if none are provided
     */
    Object[] getArguments();

    /**
     * Returns the default message to be used if the localized message is not found.
     * <p>
     * This fallback message ensures that a meaningful message is displayed
     * even when localization data is missing.
     * </p>
     *
     * @return the default message, or {@code null} if not specified
     */
    String getDefaultMessage();

    /**
     * Default implementation of {@link LocalizableMessage}.
     * <p>
     * This class provides a basic implementation that stores the message key,
     * optional arguments, and a default message for fallback scenarios.
     * </p>
     */
    class Default implements LocalizableMessage {

        private final String   defaultMessage;
        private final String[] codes;
        private final Object[] arguments;

        /**
         * Constructs a new {@code Default} instance of {@link LocalizableMessage}.
         *
         * @param codes          the message key used for localization
         * @param defaultMessage the fallback message if localization is unavailable
         * @param arguments      optional arguments for message formatting
         */
        public Default(String[] codes, String defaultMessage, Object... arguments) {
            this.codes = codes;
            this.defaultMessage = defaultMessage;
            this.arguments = arguments;
        }

        /**
         * Returns the message codes used for localization.
         * <p>
         * The key is typically used to retrieve the localized message from a message source.
         * </p>
         *
         * @return the message key
         */
        @Override
        public String[] getCodes() {
            return codes;
        }

        /**
         * Returns the arguments used for formatting the localized message.
         * <p>
         * These arguments are applied to the resolved message using a formatting mechanism
         * such as {@link MessageFormat}.
         * </p>
         *
         * @return an array of arguments for message formatting, or an empty array if none are provided
         */
        @Override
        public Object[] getArguments() {
            return arguments;
        }

        /**
         * Returns the default message to be used if the localized message is not found.
         * <p>
         * This fallback message ensures that a meaningful message is displayed
         * even when localization data is missing.
         * </p>
         *
         * @return the default message, or {@code null} if not specified
         */
        @Override
        public String getDefaultMessage() {
            return defaultMessage;
        }
    }
}
