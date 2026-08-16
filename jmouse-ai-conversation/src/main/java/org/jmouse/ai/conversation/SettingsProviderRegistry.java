package org.jmouse.ai.conversation;

import org.jmouse.ai.provider.ProviderCatalog;
import org.jmouse.ai.provider.ProviderSettings;
import org.jmouse.ai.provider.ProviderSettingsSource;
import org.jmouse.ai.view.ProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * The provider read port, answered from the settings the model is actually called with.
 *
 * <p><strong>Here rather than in either module it joins, and that placement is the design.</strong>
 * {@code jmouse-ai} declares {@link ProviderRegistry} in plain text and numbers so that it needs to
 * know nothing about providers; {@code jmouse-ai-provider} knows nothing about tools or read ports.
 * This module is the one place the two mechanisms already meet, so it is the one place that can hold
 * the translation without joining anything that was deliberately kept apart.
 *
 * <p>⚠️ <strong>The key is reduced to a boolean here, at the boundary</strong>, and never travels
 * further. {@link ProviderSettings} carries the real credential because it is about to authenticate
 * with it; everything downstream of this class gets {@code keyConfigured} and nothing else. There is no
 * second place to remember this rule, which is why it is worth a class rather than a lambda.
 */
public final class SettingsProviderRegistry implements ProviderRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsProviderRegistry.class);

    private final ProviderSettingsSource settingsSource;

    public SettingsProviderRegistry(ProviderSettingsSource settingsSource) {
        this.settingsSource = settingsSource;
    }

    @Override
    public Optional<ActiveProvider> active() {
        try {
            return Optional.ofNullable(settingsSource.settings()).map(SettingsProviderRegistry::withoutKey);

        } catch (RuntimeException unconfigured) {
            // ⚠️ Empty rather than propagating. A settings source is free to refuse — none configured,
            // two rows marked active — and a screen asking "what is in force" is precisely where that
            // has to render as an answer rather than as a page that will not load. The sentence is
            // logged so it is not lost; what a person reads on the screen is "nothing is configured".
            LOGGER.info("No AI provider is readable: {}", unconfigured.getMessage());

            return Optional.empty();
        }
    }

    /**
     * ⚠️ <strong>Usable is computed here because this is where the catalogue is visible.</strong> Whether
     * a configuration could actually send a call is "has a key OR needs none", and the second half is
     * the provider's fact rather than the settings'. Leaving it to each caller is what produced an
     * assistant that reported itself off while a local model sat there able to answer.
     */
    private static ActiveProvider withoutKey(ProviderSettings settings) {
        boolean needsKey = ProviderCatalog.requiresKey(settings.providerName());

        return new ActiveProvider(
                settings.providerName(),
                settings.model(),
                settings.apiUrl(),
                settings.maximumTokens(),
                settings.hasApiKey(),
                settings.hasApiKey() || !needsKey);
    }
}
