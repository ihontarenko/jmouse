package org.jmouse.ai.spring;

import org.jmouse.ai.ToolDispatcher;
import org.jmouse.ai.conversation.ConversationBudget;
import org.jmouse.ai.conversation.ConversationRunner;
import org.jmouse.ai.conversation.SettingsProviderRegistry;
import org.jmouse.ai.provider.ChatModel;
import org.jmouse.ai.provider.ProviderSettingsSource;
import org.jmouse.ai.view.ProviderRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * The loop that lets a model use the tools.
 *
 * <p>The one module where the two mechanisms meet — a catalogue of things that can be done, and
 * something that decides which of them to do — so it is also the one place that can hold the translation
 * between them without joining anything that was deliberately kept apart.
 *
 * <p>Registered after the provider's, because a conversation with no model is not a conversation; and
 * before the core's, so the {@link ProviderRegistry} declared here is the one a management screen reads
 * rather than the empty default.
 *
 * <p>⚠️ {@link ConversationBudget} counts <em>tokens as well as rounds</em>, and both matter. A round
 * limit alone stops a loop; it does nothing about a single round carrying a hundred thousand tokens of
 * conversation, which is the same bill arriving in a different shape.
 */
@AutoConfiguration(after = AiProviderAutoConfiguration.class, before = AiAutoConfiguration.class)
@ConditionalOnClass({ConversationRunner.class, ChatModel.class})
@EnableConfigurationProperties(AiProperties.class)
public class AiConversationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConversationBudget aiConversationBudget(AiProperties properties) {
        AiProperties.Conversation conversation = properties.getConversation();

        return new ConversationBudget(conversation.getMaximumRounds(), conversation.getMaximumTokens());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ChatModel.class)
    public ConversationRunner aiConversationRunner(
            ChatModel model, ToolDispatcher dispatcher, ConversationBudget budget) {

        return new ConversationRunner(model, dispatcher, budget);
    }

    /**
     * What a management screen may know about the provider — which is everything except the key.
     *
     * <p>The reduction happens here, at the boundary, and nothing downstream ever holds the credential.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ProviderSettingsSource.class)
    public ProviderRegistry aiProviderRegistry(ProviderSettingsSource settingsSource) {
        return new SettingsProviderRegistry(settingsSource);
    }
}
