package org.jmouse.ai.provider;

/**
 * Where the settings come from, asked once per call.
 *
 * <p>⚠️ <strong>Per call is the whole point, and binding it at startup is the mistake it exists to
 * prevent.</strong> An administrator who rotates a key, raises a token ceiling or switches provider
 * expects the next request to use it; settings resolved into a bean at startup mean a restart instead,
 * and a restart is exactly what nobody does at the moment a key is discovered to be leaking.
 *
 * <p>An implementation reading a database row every call is not the problem it looks like — one query
 * against a single row, beside an HTTP call to a language model that will take a thousand times longer.
 * An implementation that wants a cache is free to be one; a caller cannot tell the difference, which is
 * why the caching decision belongs behind this interface rather than in front of it.
 */
@FunctionalInterface
public interface ProviderSettingsSource {

    /**
     * @return the settings in force right now
     * @throws ProviderException when there are none, with a message saying what to configure and where
     */
    ProviderSettings settings();

    /** One fixed set, for a test and for a product that configures its model in a properties file. */
    static ProviderSettingsSource fixed(ProviderSettings settings) {
        return () -> settings;
    }
}
