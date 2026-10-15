package org.jmouse.ai.jpa.credential;

/**
 * The contexts this library seals credentials for — one row each.
 *
 * <p>Named here, once, because sealing and opening must spell a context identically: a context built two
 * different ways in two places is a credential that opens in neither. A product names its own rows the
 * same way, {@code <kind>:<identifier>}.
 */
public final class CredentialContexts {

    private CredentialContexts() {
    }

    /** One {@code ai_provider_settings} row's key. */
    public static String providerConfiguration(String configurationId) {
        return "ai-provider:" + configurationId;
    }
}
