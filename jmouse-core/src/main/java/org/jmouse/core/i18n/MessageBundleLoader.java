package org.jmouse.core.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Functional interface for loading localized message bundles.
 * <p>
 * Implementations of this interface are responsible for retrieving message bundles
 * based on a given basename, locale, and class loader.
 * </p>
 *
 * <p>
 * The loaded {@link MessageBundle} contains key-value mappings for localized messages,
 * typically sourced from properties files or other external resources.
 * </p>
 *
 * @see MessageBundle
 */
@FunctionalInterface
public interface MessageBundleLoader {

    /**
     * Logger instance for tracking loading operations and errors.
     */
    Logger LOGGER = LoggerFactory.getLogger(MessageBundleLoader.class);

    /**
     * Loads a message bundle for the specified basename, locale, and class loader.
     * <p>
     * The basename typically corresponds to the base name of a resource bundle,
     * such as {@code "messages"} or {@code "errors"}. The locale determines
     * the language and region, while the class loader is used to locate the bundle.
     * </p>
     *
     * @param basename    the base name of the message bundle (e.g., "messages", "labels")
     * @param locale      the locale for which the message bundle should be loaded
     * @param classLoader the class loader used to locate the resource bundle
     * @return the loaded {@link MessageBundle}, or a fallback/default bundle if loading fails
     */
    MessageBundle loadBundle(String basename, Locale locale, ClassLoader classLoader);
}
