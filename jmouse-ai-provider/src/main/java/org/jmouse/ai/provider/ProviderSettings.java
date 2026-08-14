package org.jmouse.ai.provider;

/**
 * Which model, reached how, on whose key.
 *
 * <p>A value type and nothing else — no annotations, no identity, no timestamps. Where these come from
 * is {@link ProviderSettingsSource}'s business: a row an administrator edits, a properties file, a
 * secret manager, or three lines in a test. The implementation this was learned from made it a JPA
 * entity, which meant every consumer of the settings also consumed a persistence provider.
 *
 * @param providerName which model these are for; a {@link ChatModel} refuses settings addressed to
 *                     another one rather than talking to the wrong endpoint with the right key
 * @param model        the model identifier, as the provider spells it
 * @param apiKey       the credential
 * @param apiUrl       where to send it, or null to use the provider's own default
 * @param maximumTokens    the ceiling on one answer
 */
public record ProviderSettings(
        String providerName,
        String model,
        String apiKey,
        String apiUrl,
        int    maximumTokens
) {

    /** Enough to talk to a provider at its own address. */
    public static ProviderSettings of(String providerName, String model, String apiKey, int maximumTokens) {
        return new ProviderSettings(providerName, model, apiKey, null, maximumTokens);
    }

    public ProviderSettings withApiUrl(String apiUrl) {
        return new ProviderSettings(providerName, model, apiKey, apiUrl, maximumTokens);
    }

    /** Where to send the call: what was configured, or the provider's own address. */
    public String apiUrlOr(String providerDefault) {
        return apiUrl == null || apiUrl.isBlank() ? providerDefault : apiUrl;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
