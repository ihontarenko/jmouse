package org.jmouse.notifier.spring.autoconfigure;

import org.jmouse.notifier.spring.NotifierSettings;
import org.jmouse.notifier.telegram.BindingsAddressBook;
import org.jmouse.notifier.telegram.TelegramAddressBook;
import org.jmouse.notifier.telegram.TelegramChannel;
import org.jmouse.notifier.web.Inbox;
import org.jmouse.notifier.web.MemoryInbox;
import org.jmouse.notifier.web.ScreenChannel;
import org.jmouse.telegram.TelegramGateway;
import org.jmouse.telegram.jpa.TelegramBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * ⚙️ The channels whose modules are on the classpath — each a {@code Channel} bean the notifier collects.
 */
@AutoConfiguration(afterName = "org.jmouse.telegram.spring.autoconfigure.TelegramAutoConfiguration")
public class NotifierChannelsAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifierChannelsAutoConfiguration.class);

    /**
     * ⚙️ Telegram, when {@code jmouse-notifier-telegram} is present and the application has a gateway.
     *
     * <p>⚠️ Sends as {@code jmouse.notifier.telegram.purpose} (default {@code notifications}) — the shared
     * notification bot, while the product's own bot receives its commands under another purpose.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({TelegramChannel.class, TelegramGateway.class})
    @ConditionalOnBean(TelegramGateway.class)
    @ConditionalOnProperty(name = "jmouse.notifier.telegram.enabled", havingValue = "true", matchIfMissing = true)
    static class TelegramChannelConfiguration {

        /** ⚙️ The person bindings, when {@code jmouse-telegram-jpa} keeps them; otherwise nobody — and said so. */
        @Bean
        @ConditionalOnMissingBean
        TelegramAddressBook notifierTelegramAddressBook(ObjectProvider<TelegramBindings> bindings) {
            TelegramBindings available = bindings.getIfAvailable();

            if (available == null) {
                LOGGER.warn("jmouse-notifier's Telegram channel has no person bindings (jmouse-telegram-jpa); "
                            + "it can reach only Recipient.Direct chats until a TelegramAddressBook is declared");
                return TelegramAddressBook.nobody();
            }

            return new BindingsAddressBook(available);
        }

        @Bean
        @ConditionalOnMissingBean
        TelegramChannel notifierTelegramChannel(
                TelegramGateway gateway, TelegramAddressBook addressBook, NotifierSettings settings) {
            return new TelegramChannel(gateway, settings.telegram().purpose(), addressBook);
        }
    }

    /** ⚙️ The screen, when {@code jmouse-notifier-web} is present — an inbox the product's route collects from. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ScreenChannel.class)
    @ConditionalOnProperty(name = "jmouse.notifier.screen.enabled", havingValue = "true", matchIfMissing = true)
    static class ScreenChannelConfiguration {

        @Bean
        @ConditionalOnMissingBean
        Inbox notifierInbox(NotifierSettings settings) {
            return new MemoryInbox(settings.screen().capacity(), settings.screen().lifetime(), Clock.systemUTC());
        }

        @Bean
        @ConditionalOnMissingBean
        ScreenChannel notifierScreenChannel(Inbox inbox) {
            return new ScreenChannel(inbox);
        }
    }
}
