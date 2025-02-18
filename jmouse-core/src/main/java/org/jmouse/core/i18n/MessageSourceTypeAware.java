package org.jmouse.core.i18n;

import java.util.Locale;

/**
 * An extension of {@link MessageSource} that provides message retrieval
 * based on the type of an instance or class.
 * <p>
 * This interface allows messages to be resolved using class-based keys,
 * making it easier to organize localized messages per class.
 * </p>
 *
 * @see MessageSource
 */
public interface MessageSourceTypeAware extends MessageSource {

    /**
     * Retrieves a localized message for the given instance and key.
     * <p>
     * This method derives the message key using the instance's class name.
     * </p>
     *
     * @param instance  the structured whose class name is used for message retrieval
     * @param key       the message key
     * @param locale    the target locale
     * @param arguments optional arguments for message formatting
     * @return the localized message, formatted if necessary
     */
    default String getMessageFor(Object instance, String key, Locale locale, Object... arguments) {
        return getMessageFor(instance.getClass(), key, locale, arguments);
    }

    /**
     * Retrieves a localized message for the given instance and key, using the default locale.
     * <p>
     * This method derives the message key using the instance's class name.
     * </p>
     *
     * @param instance  the structured whose class name is used for message retrieval
     * @param key       the message key
     * @param arguments optional arguments for message formatting
     * @return the localized message, formatted if necessary
     */
    default String getMessageFor(Object instance, String key, Object... arguments) {
        return getMessageFor(instance.getClass(), key, arguments);
    }

    /**
     * Retrieves a localized message for the given class and key.
     * <p>
     * The message key is prefixed with the class's simple name,
     * providing a structured namespace for localization keys.
     * </p>
     *
     * @param type      the class whose name is used as a prefix for the message key
     * @param key       the message key
     * @param locale    the target locale
     * @param arguments optional arguments for message formatting
     * @return the localized message, formatted if necessary
     */
    default String getMessageFor(Class<?> type, String key, Locale locale, Object... arguments) {
        return getMessage(type.getSimpleName() + "." + key, locale, arguments);
    }

    /**
     * Retrieves a localized message for the given class and key, using the default locale.
     * <p>
     * The message key is prefixed with the class's simple name,
     * providing a structured namespace for localization keys.
     * </p>
     *
     * @param type      the class whose name is used as a prefix for the message key
     * @param key       the message key
     * @param arguments optional arguments for message formatting
     * @return the localized message, formatted if necessary
     */
    default String getMessageFor(Class<?> type, String key, Object... arguments) {
        return getMessage(type.getSimpleName() + "." + key, arguments);
    }
}
