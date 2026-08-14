package org.jmouse.ai.spring;

import org.jmouse.ai.provider.AnthropicChatModel;
import org.jmouse.ai.provider.ChatModel;
import org.jmouse.ai.provider.GatewayChatModel;
import org.jmouse.ai.provider.OpenAiChatModel;
import org.jmouse.ai.provider.ProviderSettings;
import org.jmouse.ai.provider.ProviderSettingsSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Locale;

/**
 * Which model answers, and how it authenticates.
 *
 * <p>Present only where {@code jmouse-ai-provider} is, and contributing nothing at all until a provider
 * is named. <strong>Tools without a model provider is a supported arrangement</strong>, and the common
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
@ConditionalOnProperty(name = "jmouse.ai.provider.name")
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
    public ProviderSettingsSource aiProviderSettingsSource(AiProperties properties) {
        AiProperties.Provider provider = properties.getProvider();

        return ProviderSettingsSource.fixed(new ProviderSettings(
                normalised(provider.getName()),
                provider.getModel(),
                provider.getApiKey(),
                provider.getApiUrl(),
                provider.getMaximumTokens()));
    }

    /**
     * The model, chosen by name.
     *
     * <p>An unknown name refuses at startup rather than leaving the application without a provider and
     * no explanation — a typo here otherwise surfaces as an assistant that silently cannot answer.
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatModel aiChatModel(ProviderSettingsSource settingsSource, AiProperties properties) {
        String named = normalised(properties.getProvider().getName());

        return switch (named) {
            case AnthropicChatModel.PROVIDER_NAME -> new AnthropicChatModel(settingsSource);
            case OpenAiChatModel.PROVIDER_NAME    -> new OpenAiChatModel(settingsSource);
            case GatewayChatModel.PROVIDER_NAME   -> new GatewayChatModel(settingsSource);
            default -> throw new IllegalStateException(
                    "'jmouse.ai.provider.name' is '" + named + "', which is not a provider this library "
                    + "ships. Available: " + String.join(", ", available()) + ". Leave the property "
                    + "unset for an application that has tools and no model — that is a supported "
                    + "arrangement, not a mistake.");
        };
    }

    private static List<String> available() {
        return List.of(
                AnthropicChatModel.PROVIDER_NAME,
                OpenAiChatModel.PROVIDER_NAME,
                GatewayChatModel.PROVIDER_NAME);
    }

    private static String normalised(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
