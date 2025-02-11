package org.jmouse.core.i18n;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * An abstract implementation of the {@link MessageSource} interface.
 * <p>
 * This class provides common functionality for message resolution,
 * including support for localization, argument formatting, and fallback strategies.
 * </p>
 *
 * <p>
 * Subclasses must implement {@link #resolveMessage(String, Locale)}
 * to define how messages are retrieved.
 * </p>
 *
 * @see MessageSource
 * @see LocaleResolver
 */
public abstract class AbstractMessageSource implements MessageSource {

    /**
     * The default locale used for message retrieval if no specific locale is provided.
     */
    private Locale defaultLocale;

    /**
     * The resolver used to determine the locale when no explicit locale is set.
     */
    private LocaleResolver localeResolver = LocaleResolver.DEFAULT;

    /**
     * Whether to return the message key as a fallback if no message is found.
     */
    private boolean fallbackWithCode = false;

    /**
     * The pattern used for fallback messages when {@code fallbackWithCode} is enabled.
     * Defaults to {@code "%s"} where {@code %s} will be replaced with the message key.
     */
    private String fallbackPattern = "%s";

    /**
     * Retrieves a localized message for the given key and locale, applying argument formatting.
     * <p>
     * If a message is found, it is formatted using {@link MessageFormat} with the provided arguments.
     * If no message is found and fallback is enabled, the message key itself is returned using
     * the specified fallback pattern.
     * </p>
     *
     * @param key       the message key
     * @param locale    the target locale for message retrieval
     * @param arguments optional arguments for message formatting
     * @return the localized message, formatted if necessary, or the fallback message if not found
     */
    @Override
    public String getMessage(String key, Locale locale, Object... arguments) {
        MessageFormat messageFormat = resolveMessage(key, locale);
        String message = null;

        if (messageFormat != null) {
            synchronized (messageFormat) { // Ensure thread safety when formatting messages
                message = messageFormat.format(resolveArguments(arguments, locale));
            }
        }

        // Apply fallback strategy if the message is not found
        if (message == null && isFallbackWithCode()) {
            message = getFallbackPattern().formatted(key);
        }

        return message;
    }

    /**
     * Retrieves a localized message for the given key using the default locale.
     *
     * @param key       the message key
     * @param arguments optional arguments for message formatting
     * @return the localized message, formatted if necessary
     */
    @Override
    public String getMessage(String key, Object... arguments) {
        return getMessage(key, getDefaultLocale(), arguments);
    }

    /**
     * Resolves arguments before applying them to the message format.
     * <p>
     * Subclasses may override this method to preprocess arguments (e.g., localization).
     * </p>
     *
     * @param arguments the original arguments
     * @param locale    the target locale
     * @return the processed arguments
     */
    protected Object[] resolveArguments(Object[] arguments, Locale locale) {
        return arguments;
    }

    /**
     * Resolves the message format for the given key and locale.
     * <p>
     * This method must be implemented by subclasses to provide the actual message retrieval logic.
     * </p>
     *
     * @param key    the message key
     * @param locale the target locale
     * @return the resolved {@link MessageFormat}, or {@code null} if not found
     */
    protected abstract MessageFormat resolveMessage(String key, Locale locale);

    /**
     * Checks whether the message source should return the message key as a fallback.
     *
     * @return {@code true} if fallback is enabled, otherwise {@code false}
     */
    public boolean isFallbackWithCode() {
        return fallbackWithCode;
    }

    /**
     * Sets whether the message source should return the message key as a fallback.
     *
     * @param fallbackWithCode {@code true} to enable fallback, otherwise {@code false}
     */
    public void setFallbackWithCode(boolean fallbackWithCode) {
        this.fallbackWithCode = fallbackWithCode;
    }

    /**
     * Returns the default locale used when no locale is explicitly specified.
     *
     * @return the default locale
     */
    public Locale getDefaultLocale() {
        return defaultLocale == null ? localeResolver.getLocale() : defaultLocale;
    }

    /**
     * Sets the default locale for message retrieval.
     *
     * @param defaultLocale the locale to set as default
     */
    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    /**
     * Returns the pattern used for fallback messages when a message is not found.
     *
     * @return the fallback pattern
     */
    public String getFallbackPattern() {
        return fallbackPattern;
    }

    /**
     * Sets the pattern used for fallback messages.
     *
     * @param fallbackPattern the new fallback pattern
     */
    public void setFallbackPattern(String fallbackPattern) {
        this.fallbackPattern = fallbackPattern;
    }

    /**
     * Returns the current locale resolver used by this message source.
     *
     * @return the current {@link LocaleResolver}
     */
    public LocaleResolver getLocaleResolver() {
        return localeResolver;
    }

    /**
     * Sets the locale resolver for this message source.
     *
     * @param localeResolver the {@link LocaleResolver} to use
     */
    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }
}
