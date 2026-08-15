package org.jmouse.ai.spring;

import org.jmouse.ai.provider.ChatModel;
import org.jmouse.ai.provider.ProviderCatalog;
import org.jmouse.ai.provider.ProviderSettings;
import org.jmouse.ai.provider.ProviderSettingsSource;
import org.jmouse.ai.provider.RoutingChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Which model answers, and how it authenticates.
 *
 * <p>Present only where {@code jmouse-ai-provider} is, and contributing nothing at all until either a
 * provider is named or a settings source exists. <strong>Tools without a model provider is a supported arrangement</strong>, and the common
 * one: an application serving the Model Context Protocol has no use for a chat model, because the model
 * is on the other end of the connection.
 *
 * <h2>Which implementation, and which settings, are different questions</h2>
 *
 * <p>⚠️ {@code jmouse.ai.provider.name} always chooses the <em>implementation</em>, even when the
 * settings themselves come from the database. That split is deliberate: which vendor's wire format this
 * application speaks is a deploy-time fact and changing it is a deployment; which model and which key it
 * uses is an operational one and should be changeable without a release. Reading the implementation
 * choice out of a table would mean a database row deciding which classes get instantiated at startup,
 * and a wrong row taking the application down before anything could report it.
 *
 * <p>{@code HttpChatModel} checks the two agree before it sends anything, so settings addressed at one
 * provider can never reach another's endpoint — which would otherwise come back as an authentication
 * failure and read as a bad key.
 */
@AutoConfiguration(before = AiAutoConfiguration.class)
@ConditionalOnClass(ChatModel.class)
@EnableConfigurationProperties(AiProperties.class)
public class AiProviderAutoConfiguration {

    /**
     * Settings from configuration.
     *
     * <p>⚠️ Conditional on there being no other source, which is how the database-backed one wins where
     * it is switched on: it is registered by an autoconfiguration ordered before this.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "jmouse.ai.provider.name")
    public ProviderSettingsSource aiProviderSettingsSource(AiProperties properties) {
        AiProperties.Provider provider = properties.getProvider();

        return ProviderSettingsSource.fixed(new ProviderSettings(
                ProviderCatalog.normalised(provider.getName()),
                provider.getModel(),
                provider.getApiKey(),
                provider.getApiUrl(),
                provider.getMaximumTokens()));
    }

    /**
     * The model — named by a property where there is one, and read from the settings in force where
     * there is not.
     *
     * <p>⚠️ <strong>Conditional on a settings source existing, which is what "tools and no model" looks
     * like.</strong> With neither {@code jmouse.ai.provider.name} nor a database-backed source there is
     * no bean here at all, and an application serving only the Model Context Protocol starts perfectly
     * — the model is on the other end of that connection.
     *
     * <h2>Two arrangements, and which one applies</h2>
     *
     * <p><strong>A property names the implementation.</strong> Then it is chosen here, once, and an
     * unknown name refuses at startup rather than surfacing later as an assistant that silently cannot
     * answer. That remains true even where the <em>settings</em> come from a table: which vendor's wire
     * format this application speaks is a deploy-time fact.
     *
     * <p><strong>No property, settings from a table.</strong> Then the row carries the provider name
     * too, and {@link RoutingChatModel} reads it per turn — so switching provider is a form rather than
     * a restart. ⚠️ This is what a management screen needs to be worth having, and it was written out in
     * two products before it was written here.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ProviderSettingsSource.class)
    public ChatModel aiChatModel(ProviderSettingsSource settingsSource, AiProperties properties) {
        String named = ProviderCatalog.normalised(properties.getProvider().getName());

        if (named.isBlank()) {
            return RoutingChatModel.overShippedProviders(settingsSource);
        }

        return ProviderCatalog.modelFor(named, settingsSource).orElseThrow(
                () -> new IllegalStateException(
                        "'jmouse.ai.provider.name' is '" + named + "', which is not a provider this "
                        + "library ships. Available: " + String.join(", ", ProviderCatalog.shipped())
                        + ". Leave the property unset to let the settings in force name the provider, "
                        + "or unset it entirely for an application that has tools and no model — both "
                        + "are supported arrangements rather than mistakes."));
    }
}
