package org.jmouse.core.i18n;

import java.util.Locale;

/**
 * Defines a contract for retrieving localized messages.
 * <p>
 * Implementations of this interface provide access to messages based on a
 * message key and optional arguments. This is commonly used for internationalization (i18n)
 * and localization (l10n) in applications.
 * </p>
 *
 * @see MessageSourceBundle
 */
public interface MessageSource {

    /**
     * Retrieves a localized message for the given key and locale.
     * <p>
     * If the message contains placeholders, the provided arguments will be
     * used to format the message accordingly.
     * </p>
     *
     * @param key       the message key (e.g., "error.not.found", "label.username")
     * @param locale    the locale to retrieve the message for
     * @param arguments optional arguments for message formatting
     * @return the localized message, formatted if arguments are provided
     */
    String getMessage(String key, Locale locale, Object... arguments);

    /**
     * Retrieves a localized message for the given key using the default locale.
     * <p>
     * This method is useful when locale management is handled externally or when
     * the application assumes a single default locale.
     * </p>
     *
     * @param key       the message key (e.g., "welcome.message", "button.submit")
     * @param arguments optional arguments for message formatting
     * @return the localized message, formatted if arguments are provided
     */
    String getMessage(String key, Object... arguments);
}
