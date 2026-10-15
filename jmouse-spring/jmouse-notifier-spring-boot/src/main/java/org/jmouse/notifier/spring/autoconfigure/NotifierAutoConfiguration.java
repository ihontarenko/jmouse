package org.jmouse.notifier.spring.autoconfigure;

import org.jmouse.notifier.Channel;
import org.jmouse.notifier.InMemoryNotificationStore;
import org.jmouse.notifier.NotificationStore;
import org.jmouse.notifier.Notifier;
import org.jmouse.notifier.RecipientResolver;
import org.jmouse.notifier.spring.NotifierRetryWorker;
import org.jmouse.notifier.spring.NotifierSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ⚙️ The notifier — one store, every channel the classpath brings, and a retry worker.
 *
 * <p>Runs after the channel configurations, so every {@link Channel} bean they declared is in the list.
 *
 * <h2>⚠️ The product must supply a {@link RecipientResolver}</h2>
 *
 * <p>Only the product knows who "everyone in this household" is. Without one, startup fails with a
 * sentence — a default resolving to nobody would make every notification vanish without a trace.
 */
@AutoConfiguration(after = {NotifierChannelsAutoConfiguration.class, NotifierJpaAutoConfiguration.class})
@ConditionalOnClass(Notifier.class)
public class NotifierAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifierAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public NotifierSettings notifierSettings(Environment environment) {
        return Binder.get(environment).bind("jmouse.notifier", NotifierSettings.class).orElseGet(NotifierSettings::defaults);
    }

    /**
     * ⚙️ In memory unless {@code jmouse.notifier.store=database}, which {@link NotifierJpaAutoConfiguration}
     * supplies. Asked for and impossible, it refuses to start rather than quietly keeping retries in memory.
     */
    @Bean
    @ConditionalOnMissingBean
    public NotificationStore notificationStore(NotifierSettings settings) {
        if (settings.store() == NotifierSettings.Store.DATABASE) {
            throw new IllegalStateException(
                    "jmouse.notifier.store=database needs jmouse-notifier-jpa and a JPA EntityManagerFactory on the classpath");
        }

        return new InMemoryNotificationStore(settings.capacity());
    }

    @Bean
    @ConditionalOnMissingBean
    public Notifier notifier(
            NotificationStore store, ObjectProvider<RecipientResolver> resolver, List<Channel> channels,
            NotifierSettings settings) {
        RecipientResolver available = resolver.getIfAvailable();

        if (available == null) {
            throw new IllegalStateException(
                    "jmouse-notifier needs a RecipientResolver bean: only the product knows who a Recipient means. "
                    + "Declare one that turns Recipient.Subject, Group, Everyone and Device into Addressees.");
        }

        LOGGER.info("jmouse-notifier ready: store={}, channels=[{}]", store.getClass().getSimpleName(),
                    channels.stream().map(Channel::name).collect(Collectors.joining(", ")));

        if (channels.isEmpty()) {
            LOGGER.warn("jmouse-notifier has no channel; every publication will reach nobody. "
                        + "Add jmouse-notifier-telegram or jmouse-notifier-web, or declare a Channel bean");
        }

        return new Notifier(store, available, channels, Clock.systemUTC(), settings.maximumAttempts());
    }

    @Bean(initMethod = "start")
    @ConditionalOnMissingBean
    public NotifierRetryWorker notifierRetryWorker(Notifier notifier, NotifierSettings settings) {
        return new NotifierRetryWorker(notifier, settings.retryInterval());
    }
}
