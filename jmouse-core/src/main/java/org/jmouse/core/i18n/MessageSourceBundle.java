package org.jmouse.core.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Represents a source for loading localized message bundles.
 * <p>
 * Implementations of this interface provide access to message bundles
 * based on a given basename and locale. This is typically used for
 * internationalization (i18n) and localization (l10n) support.
 * </p>
 *
 * @see MessageBundle
 */
public interface MessageSourceBundle {

    /**
     * Logger instance for tracking loading operations and errors.
     */
    Logger LOGGER = LoggerFactory.getLogger(MessageSourceBundle.class);

    /**
     * Retrieves a message bundle for the specified basename and locale.
     * <p>
     * The basename typically represents the base name of a resource bundle,
     * while the locale specifies the target language and region.
     * </p>
     *
     * @param basename the base name of the message bundle (e.g., "messages", "errors")
     * @param locale   the locale for which the messages should be retrieved
     * @return the corresponding {@link MessageBundle} instance
     */
    MessageBundle getBundle(String basename, Locale locale);
}
