package org.jmouse.telegram.spring.autoconfigure;

import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.bot.BotApiTransport;
import org.jmouse.telegram.bot.LongPolling;
import org.jmouse.telegram.spring.TelegramSettings;
import org.jmouse.telegram.update.UpdateDispatcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.Set;

/**
 * ⚙️ The polling loop, when {@code jmouse.telegram.updates.mode=polling}.
 *
 * <p>⚠️ Conditional on the property, so no thread exists in an application that only sends. That is
 * not tidiness: a polling loop in a product nobody configured for it would silently take the bot's
 * updates away from whatever was supposed to receive them — Telegram delivers to exactly one consumer.
 *
 * <p>⚠️ <strong>Polling is what works on a development machine.</strong> A webhook needs a publicly
 * reachable HTTPS address, and this workspace's host serves plain HTTP from behind a router. So the
 * mode is expected to differ between environments, which is exactly why it is a property and not a
 * decision taken in code.
 */
@AutoConfiguration(after = TelegramAutoConfiguration.class)
@ConditionalOnProperty(name = "jmouse.telegram.updates.mode", havingValue = "polling")
public class TelegramPollingAutoConfiguration {

    /**
     * ⚙️ Started and — the half that is usually forgotten — <strong>stopped</strong> with the context.
     *
     * <p>The bean is returned as a {@link LongPolling}, which is {@link AutoCloseable}, so Spring calls
     * {@code close()} on shutdown by itself. ⚠️ That matters most in development: without it, a restart
     * leaves the previous loop alive and holding the bot's updates, and the new one appears to receive
     * roughly half of everything at random.
     */
    @Bean(initMethod = "start")
    @ConditionalOnMissingBean
    public LongPolling telegramLongPolling(
            BotApiTransport  transport,
            IdentitySource   identities,
            UpdateDispatcher dispatcher,
            TelegramSettings settings) {

        return new LongPolling(
                transport,
                identities,
                dispatcher,
                IdentitySource.GENERAL,
                settings.updates().pollTimeout(),
                LongPolling.DEFAULT_FAILURE_BACKOFF,
                Set.copyOf(settings.updates().allowed()));
    }
}
