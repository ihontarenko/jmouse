package org.jmouse.core.i18n;

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

    static LocalizableMessage of(String key, String defaultMessage, Object... arguments) {
        return new LocalizableMessage.Default(key, defaultMessage, arguments);
    }

    static LocalizableMessage of(String key, Object... arguments) {
        return of(key, null, arguments);
    }

    /**
     * Returns the message key used for localization.
     * <p>
     * The key is typically used to retrieve the localized message from a message source.
     * </p>
     *
     * @return the message key
     */
    String getMessageKey();

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
        private final String   key;
        private final Object[] arguments;

        /**
         * Constructs a new {@code Default} instance of {@link LocalizableMessage}.
         *
         * @param key            the message key used for localization
         * @param defaultMessage the fallback message if localization is unavailable
         * @param arguments      optional arguments for message formatting
         */
        public Default(String key, String defaultMessage, Object... arguments) {
            this.key = key;
            this.arguments = arguments;
            this.defaultMessage = defaultMessage;
        }

        /**
         * Returns the message key used for localization.
         * <p>
         * The key is typically used to retrieve the localized message from a message source.
         * </p>
         *
         * @return the message key
         */
        @Override
        public String getMessageKey() {
            return key;
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
