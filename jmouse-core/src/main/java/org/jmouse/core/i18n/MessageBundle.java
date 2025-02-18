package org.jmouse.core.i18n;

import java.util.Locale;
import java.util.Set;

/**
 * Represents a localized message bundle containing key-value pairs for messages.
 * <p>
 * Implementations of this interface provide access to localized messages based on
 * a given locale and key. Typically, message bundles are sourced from resource files
 * such as `.properties` or external storage.
 * </p>
 *
 * @see MessageSource
 * @see MessageBundleLoader
 */
public interface MessageBundle {

    MessageBundle NULL_OBJECT = new Empty();

    /**
     * Retrieves an structured associated with the given key from the message bundle.
     * <p>
     * This method returns a generic structured, allowing flexibility in message storage.
     * If the key does not exist, implementations should handle it gracefully.
     * </p>
     *
     * @param key the message key (e.g., "error.not.found", "button.submit")
     * @return the associated structured, or {@code null} if not found
     */
    Object getObject(String key);

    /**
     * Retrieves a string message associated with the given key from the message bundle.
     * <p>
     * This method is a convenience wrapper around {@link #getObject(String)}, ensuring
     * that the returned value is a string. If the key is not found, implementations
     * should return a default message or throw an exception.
     * </p>
     *
     * @param key the message key
     * @return the corresponding message string, or a default message if not found
     */
    String getString(String key);

    /**
     * Returns a unique identifier for this message bundle.
     * <p>
     * The bundle ID typically consists of the basename and locale, allowing
     * implementations to distinguish between different resource bundles.
     * </p>
     *
     * @return the unique bundle identifier
     */
    String getBundleId();

    /**
     * Returns a set of all keys available in this message bundle.
     * <p>
     * This method provides access to all available message keys, which can be useful
     * for debugging or dynamic message resolution.
     * </p>
     *
     * @return a set of all message keys in this bundle
     */
    Set<String> getKeys();

    /**
     * Returns the locale associated with this message bundle.
     * <p>
     * The locale determines the language and region-specific formatting
     * for messages stored in the bundle.
     * </p>
     *
     * @return the {@link Locale} of this message bundle
     */
    Locale getLocale();

    class Empty implements MessageBundle {

        @Override
        public Object getObject(String key) {
            return null;
        }

        @Override
        public String getString(String key) {
            return null;
        }

        @Override
        public String getBundleId() {
            return "[:EMPTY:]";
        }

        @Override
        public Set<String> getKeys() {
            return Set.of();
        }

        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }

    }
}
