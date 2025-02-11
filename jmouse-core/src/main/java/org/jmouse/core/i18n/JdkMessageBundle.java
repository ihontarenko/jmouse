package org.jmouse.core.i18n;

import org.jmouse.util.Priority;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;

/**
 * A {@link MessageBundle} implementation based on the JDK's {@link ResourceBundle}.
 * <p>
 * This class wraps a {@link ResourceBundle} and provides a standard way to access localized messages.
 * </p>
 *
 * @see MessageBundle
 * @see ResourceBundle
 */
public class JdkMessageBundle implements MessageBundle {

    private final ResourceBundle bundle;

    /**
     * Constructs a new {@code JdkMessageBundle} with the specified {@link ResourceBundle}.
     *
     * @param bundle the resource bundle to wrap
     */
    public JdkMessageBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    /**
     * Retrieves an object associated with the given key.
     *
     * @param key the message key
     * @return the corresponding object, or throws {@link MissingResourceException} if not found
     */
    @Override
    public Object getObject(String key) {
        return bundle.getObject(key);
    }

    /**
     * Retrieves a string message associated with the given key.
     *
     * @param key the message key
     * @return the corresponding message string, or throws {@link MissingResourceException} if not found
     */
    @Override
    public String getString(String key) {
        return bundle.getString(key);
    }

    /**
     * Returns all available keys in this message bundle.
     *
     * @return a set of all message keys
     */
    @Override
    public Set<String> getKeys() {
        return bundle.keySet();
    }

    /**
     * Returns the locale associated with this message bundle.
     *
     * @return the locale of this bundle
     */
    @Override
    public Locale getLocale() {
        return bundle.getLocale();
    }

    /**
     * Returns the unique bundle identifier, typically the base bundle name.
     *
     * @return the base bundle name
     */
    @Override
    public String getBundleId() {
        return bundle.getBaseBundleName();
    }

    /**
     * A loader implementation for loading {@link JdkMessageBundle} instances.
     * <p>
     * This loader is registered with the lowest priority to ensure that it acts as a fallback
     * when no other higher-priority message bundle loaders are available.
     * </p>
     *
     * @see MessageBundleLoader
     */
    @Priority(Integer.MIN_VALUE)
    public static class Loader implements MessageBundleLoader {

        /**
         * Loads a message bundle based on the specified base name, locale, and class loader.
         * <p>
         * If the resource bundle cannot be found, an error message is logged, and {@code null} is returned.
         * </p>
         *
         * @param basename    the base name of the resource bundle
         * @param locale      the locale for which the bundle should be loaded
         * @param classLoader the class loader used to locate the resource bundle
         * @return a {@link JdkMessageBundle} instance, or {@code null} if not found
         */
        @Override
        public MessageBundle loadBundle(String basename, Locale locale, ClassLoader classLoader) {
            MessageBundle messageBundle = null;

            try {
                ResourceBundle bundle = ResourceBundle.getBundle(basename, locale, classLoader);
                if (bundle != null) {
                    messageBundle = new JdkMessageBundle(bundle);
                }
            } catch (MissingResourceException exception) {
                LOGGER.error(exception.getMessage());
            }

            return messageBundle;
        }

        /**
         * Loads a message bundle from a {@link Reader}.
         * <p>
         * This method is useful for reading properties-based resource bundles from character streams.
         * </p>
         *
         * @param reader the reader providing the properties file content
         * @return a {@link JdkMessageBundle} instance
         * @throws IOException if an error occurs while reading the stream
         */
        public MessageBundle loadBundle(Reader reader) throws IOException {
            return new JdkMessageBundle(new PropertyResourceBundle(reader));
        }

        /**
         * Loads a message bundle from an {@link InputStream}.
         * <p>
         * This method is useful for reading properties-based resource bundles from byte streams.
         * </p>
         *
         * @param stream the input stream containing the properties file content
         * @return a {@link JdkMessageBundle} instance
         * @throws IOException if an error occurs while reading the stream
         */
        public MessageBundle loadBundle(InputStream stream) throws IOException {
            return new JdkMessageBundle(new PropertyResourceBundle(stream));
        }
    }
}
