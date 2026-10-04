package org.jmouse.telegram.spring.autoconfigure;

import org.jmouse.telegram.spring.TelegramSettings;
import org.jmouse.telegram.spring.TelegramWebhookController;
import org.jmouse.telegram.update.UpdateDispatcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;

/**
 * ⚙️ The webhook endpoint, when {@code jmouse.telegram.updates.mode=webhook}.
 *
 * <p>⚠️ Conditional on the property <em>and</em> on Spring MVC being present, so an application that
 * polls, or that only sends, never acquires a public route it did not ask for.
 */
@AutoConfiguration(after = TelegramAutoConfiguration.class)
@ConditionalOnClass(RestController.class)
@ConditionalOnProperty(name = "jmouse.telegram.updates.mode", havingValue = "webhook")
public class TelegramWebAutoConfiguration {

    /**
     * ⚙️ The endpoint.
     *
     * <h2>⚠️ A missing secret fails startup rather than being served insecurely</h2>
     *
     * <p>Without the secret this is an unauthenticated public route that accepts anything shaped like a
     * Telegram update — so anybody on the internet can make the application believe a message arrived
     * from a chat of their choosing, and every handler downstream treats it as genuine.
     *
     * <p>Defaulting to "no check" would make that the state an installation reaches by <em>forgetting</em>
     * a property, which is the worst possible way to arrive at it. Refusing to start is loud, happens
     * before any traffic, and names the property to set. An application that genuinely wants no check
     * declares its own controller bean and this one steps aside — a decision somebody has to write down.
     */
    @Bean
    @ConditionalOnMissingBean
    public TelegramWebhookController telegramWebhookController(
            UpdateDispatcher dispatcher, TelegramSettings settings) {

        TelegramSettings.Webhook webhook = settings.updates().webhook();

        if (!webhook.hasSecret()) {
            throw new IllegalStateException(
                    "jmouse.telegram.updates.mode=webhook needs jmouse.telegram.updates.webhook.secret. "
                    + "Telegram echoes it in the X-Telegram-Bot-Api-Secret-Token header, and it is the "
                    + "only thing distinguishing Telegram from anybody else who finds the address.");
        }

        return new TelegramWebhookController(dispatcher, webhook.secret());
    }
}
