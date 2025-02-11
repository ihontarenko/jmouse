package org.jmouse.core.i18n;

import org.jmouse.util.Charset;
import org.jmouse.util.Sorter;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An abstract implementation of {@link MessageSourceBundle}.
 * <p>
 * This class provides caching and loading mechanisms for localized message bundles.
 * It supports multiple message loaders and formats messages using {@link MessageFormat}.
 * </p>
 *
 * @see MessageSource
 * @see MessageSourceBundle
 * @see MessageBundle
 * @see MessageFormat
 */
public abstract class AbstractMessageSourceBundle extends AbstractMessageSource implements MessageSourceBundle {

    /**
     * A cache for storing message bundles mapped by their base names and locales.
     */
    protected final Map<String, Map<Locale, MessageBundle>> cache = new ConcurrentHashMap<>();

    /**
     * A cache for storing precompiled message formats mapped by their keys.
     */
    protected final Map<String, MessageFormat> formats = new ConcurrentHashMap<>();

    /**
     * A set of registered base names used to load message bundles.
     */
    protected final Set<String> names = new HashSet<>();

    /**
     * A list of message bundle loaders that are responsible for loading message bundles.
     */
    protected final List<MessageBundleLoader> loaders = new ArrayList<>();

    private Charset     defaultEncoding;
    private ClassLoader classLoader;

    /**
     * Constructs an {@code AbstractMessageSourceBundle} with the specified class loader.
     *
     * @param classLoader the class loader used for loading message bundles
     */
    public AbstractMessageSourceBundle(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Returns the set of registered base names for message bundles.
     *
     * @return a set of base names
     */
    public Set<String> getNames() {
        return names;
    }

    /**
     * Sets the base names for message bundles, replacing any existing names.
     *
     * @param names a set of base names to be used for loading message bundles
     */
    public void setNames(Set<String> names) {
        this.names.clear();
        this.names.addAll(names);
    }

    /**
     * Adds new base names for message bundles.
     *
     * @param names the base names to add
     */
    public void addNames(String... names) {
        this.names.addAll(Arrays.asList(names));
    }

    /**
     * Clears all registered base names.
     */
    public void clearNames() {
        setNames(Collections.emptySet());
    }

    /**
     * Returns the class loader used for loading message bundles.
     *
     * @return the class loader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets the class loader used for loading message bundles.
     *
     * @param classLoader the new class loader
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Returns the default character encoding for message bundles.
     *
     * @return the default encoding
     */
    public Charset getDefaultEncoding() {
        return defaultEncoding;
    }

    /**
     * Sets the default character encoding for message bundles.
     *
     * @param defaultEncoding the new default encoding
     */
    public void setDefaultEncoding(Charset defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

    /**
     * Retrieves a message bundle for the given base name and locale.
     * <p>
     * The result is cached to optimize performance and avoid redundant lookups.
     * </p>
     *
     * @param basename the base name of the bundle
     * @param locale   the locale for which the bundle should be retrieved
     * @return the corresponding {@link MessageBundle}, or {@code null} if not found
     */
    @Override
    public MessageBundle getBundle(String basename, Locale locale) {
        Map<Locale, MessageBundle> bundles       = cache.computeIfAbsent(basename, k -> new ConcurrentHashMap<>());
        MessageBundle              messageBundle = bundles.get(locale);

        if (messageBundle == null) {
            messageBundle = doLoadBundle(basename, locale, getClassLoader());
            if (messageBundle != null) {
                bundles.put(locale, messageBundle);
            }
        }

        return messageBundle;
    }

    /**
     * Resolves a formatted message for the given key and locale.
     *
     * @param bundle the message bundle
     * @param key    the message key
     * @param locale the target locale
     * @return the resolved {@link MessageFormat}, or {@code null} if not found
     */
    protected MessageFormat resolveFormat(MessageBundle bundle, String key, Locale locale) {
        String        cacheKey      = resolveFormatKey(bundle, key, locale);
        MessageFormat messageFormat = formats.get(cacheKey);

        if (messageFormat == null) {
            String message = getRawMessage(bundle, key);
            if (message != null) {
                messageFormat = createMessageFormat(message, locale);
                formats.put(cacheKey, messageFormat);
            }
        }

        return messageFormat;
    }

    /**
     * Retrieves the raw message string from the bundle.
     *
     * @param bundle the message bundle
     * @param key    the message key
     * @return the raw message string, or {@code null} if not found
     */
    protected String getRawMessage(MessageBundle bundle, String key) {
        String message = null;

        try {
            message = bundle.getString(key);
        } catch (MissingResourceException exception) {
            LOGGER.info("Resource '{}' in bundle '{}' not found.", key, bundle.getBundleId());
        }

        return message;
    }

    /**
     * Creates a {@link MessageFormat} instance for the given value and locale.
     *
     * @param value  the message string
     * @param locale the target locale
     * @return a new {@link MessageFormat} instance
     */
    protected MessageFormat createMessageFormat(String value, Locale locale) {
        return new MessageFormat(value, locale);
    }

    /**
     * Generates a unique key for caching message formats.
     *
     * @param bundle the message bundle
     * @param key    the message key
     * @param locale the target locale
     * @return a formatted string representing the cache key
     */
    protected String resolveFormatKey(MessageBundle bundle, String key, Locale locale) {
        StringJoiner joiner = new StringJoiner("/", "[", "]");

        if (bundle != null) {
            joiner.add(bundle.getBundleId());
        }

        if (locale != null) {
            joiner.add(locale.toString());
        }

        if (key != null) {
            joiner.add(key);
        }

        return joiner.toString();
    }

    /**
     * Resolves a message format by searching through registered message bundle names.
     *
     * @param key    the message key
     * @param locale the target locale
     * @return the corresponding {@link MessageFormat}, or {@code null} if not found
     */
    @Override
    protected MessageFormat resolveMessage(String key, Locale locale) {
        MessageFormat messageFormat = null;

        for (String basename : getNames()) {
            MessageBundle bundle = getBundle(basename, locale);
            if (bundle != null) {
                messageFormat = resolveFormat(bundle, key, locale);
                if (messageFormat != null) {
                    break;
                }
            }
        }

        return messageFormat;
    }

    /**
     * Loads a message bundle using registered loaders.
     *
     * @param basename    the base name of the bundle
     * @param locale      the target locale
     * @param classLoader the class loader to use for loading the bundle
     * @return the loaded {@link MessageBundle}, or {@code null} if none are found
     */
    protected MessageBundle doLoadBundle(String basename, Locale locale, ClassLoader classLoader) {
        Sorter.sort(loaders); // Sort loaders based on priority

        MessageBundle messageBundle = null;

        for (MessageBundleLoader loader : loaders) {
            messageBundle = loader.loadBundle(basename, locale, classLoader);
            if (messageBundle != null) {
                break;
            }
        }

        return messageBundle;
    }
}
