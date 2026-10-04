package org.jmouse.ai.provider;

import java.util.Set;
import org.jmouse.ai.model.AiCapability;

/** One call's immutable settings snapshot. Credentials must never enter diagnostics. */
public record ProviderSettings(String providerName, String model, String apiKey, String apiUrl,
        int maximumTokens, Set<AiCapability> capabilities, String configurationId, long revision) {
    public ProviderSettings {
        if (capabilities == null || capabilities.isEmpty() || revision < 0) {
            throw new IllegalArgumentException("Explicit nonempty capabilities and a nonnegative revision are required.");
        }
        capabilities = Set.copyOf(capabilities);
    }

    /** Legacy configurations supported chat only. */
    public ProviderSettings(String providerName, String model, String apiKey, String apiUrl, int maximumTokens) {
        this(providerName, model, apiKey, apiUrl, maximumTokens, Set.of(AiCapability.CHAT), null, 0);
    }

    public static ProviderSettings of(String providerName, String model, String apiKey, int maximumTokens) {
        return new ProviderSettings(providerName, model, apiKey, null, maximumTokens);
    }

    public ProviderSettings withApiUrl(String apiUrl) {
        return new ProviderSettings(providerName, model, apiKey, apiUrl, maximumTokens, capabilities, configurationId, revision);
    }

    public String apiUrlOr(String providerDefault) {
        return apiUrl == null || apiUrl.isBlank() ? providerDefault : apiUrl;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public ProviderSettings requireCapability(AiCapability capability) {
        if (capability == null || !capabilities.contains(capability)) {
            throw new ProviderException("The configured model does not declare the required capability.");
        }
        return this;
    }

    @Override
    public String toString() {
        return "ProviderSettings[providerName=" + providerName + ", model=" + model
                + ", capabilities=" + capabilities + ", configurationId=" + configurationId + ", revision=" + revision + "]";
    }
}
