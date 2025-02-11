package org.jmouse.core.i18n;

import java.util.Locale;

/**
 * A functional interface for resolving the current {@link Locale}.
 * <p>
 * Implementations of this interface provide a mechanism for dynamically determining
 * the locale to be used in message resolution. This can be based on system settings,
 * user preferences, or request context in web applications.
 * </p>
 *
 * <p>
 * A default implementation is provided, which always returns {@link Locale#ENGLISH}.
 * </p>
 *
 * @see MessageSource
 */
@FunctionalInterface
public interface LocaleResolver {

    /**
     * The default locale resolver that always returns {@link Locale#ENGLISH}.
     * <p>
     * This implementation can be overridden by a custom {@code LocaleResolver}
     * to provide dynamic locale determination.
     * </p>
     */
    LocaleResolver DEFAULT = () -> Locale.ENGLISH;

    /**
     * Resolves and returns the current {@link Locale}.
     * <p>
     * The returned locale determines which localized messages will be retrieved
     * from a {@link MessageSource}. Implementations may derive the locale from
     * different sources, such as system settings, user preferences, or HTTP requests.
     * </p>
     *
     * @return the resolved {@link Locale}, never {@code null}
     */
    Locale getLocale();
}
