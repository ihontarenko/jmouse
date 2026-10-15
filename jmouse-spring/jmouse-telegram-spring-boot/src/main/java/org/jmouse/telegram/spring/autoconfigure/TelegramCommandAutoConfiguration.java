package org.jmouse.telegram.spring.autoconfigure;

import org.jmouse.telegram.TelegramGateway;
import org.jmouse.telegram.command.BindingsSenderResolver;
import org.jmouse.telegram.command.BotCommand;
import org.jmouse.telegram.command.CommandGuard;
import org.jmouse.telegram.command.CommandRouter;
import org.jmouse.telegram.command.SenderResolver;
import org.jmouse.telegram.jpa.TelegramBindings;
import org.jmouse.telegram.menu.BotMenus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.telegram.spring.TelegramSettings;
import org.jmouse.telegram.update.UpdateDispatcher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * ⚙️ Bot commands — when {@code jmouse-telegram-command} is on the classpath and the product declared at
 * least one {@link BotCommand} bean.
 *
 * <p>Replies go out as {@code jmouse.telegram.updates.purpose} — the product's own bot, the one whose
 * updates arrive here.
 *
 * <h2>⚠️ Two things the product must decide, and startup refuses to guess</h2>
 *
 * <ul>
 *   <li><strong>A {@link CommandGuard}.</strong> Defaulting to "every bound person may run everything"
 *       would make a Telegram account a key to every command the moment one was declared.</li>
 *   <li><strong>A {@link SenderResolver}</strong> — or {@code jmouse-telegram-jpa}'s bindings, which are
 *       used when present.</li>
 * </ul>
 */
@AutoConfiguration(after = TelegramAutoConfiguration.class)
@ConditionalOnClass(CommandRouter.class)
@ConditionalOnBean(BotCommand.class)
public class TelegramCommandAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramCommandAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public CommandRouter telegramCommandRouter(
            TelegramGateway gateway, TelegramSettings settings, UpdateDispatcher dispatcher,
            List<BotCommand> commands, ObjectProvider<CommandGuard> guard,
            ObjectProvider<SenderResolver> senders, ObjectProvider<TelegramBindings> bindings) {
        CommandGuard declaredGuard = guard.getIfAvailable();

        if (declaredGuard == null) {
            throw new IllegalStateException(
                    "Bot commands are declared but no CommandGuard bean is: the product must say who may run them. "
                    + "Declare one — CommandGuard.everyone() is a deliberate choice, not a default.");
        }

        SenderResolver resolver = senders.getIfAvailable(() -> {
            TelegramBindings available = bindings.getIfAvailable();

            if (available == null) {
                throw new IllegalStateException(
                        "Bot commands need to know who a sender is: declare a SenderResolver bean, "
                        + "or add jmouse-telegram-jpa so its person bindings answer");
            }

            return new BindingsSenderResolver(available);
        });

        CommandRouter router = new CommandRouter(
                gateway.as(settings.updates().purpose()), resolver, declaredGuard, commands);

        router.install(dispatcher);

        return router;
    }

    /**
     * ⚙️ Publishes the router's commands as the bot's menu once the application is up — off by
     * {@code jmouse.telegram.commands.menu=false}.
     *
     * <p>⚠️ On a thread of its own and never fatal: Telegram being unreachable at start-up must not stop
     * the application, and a menu one restart late is harmless.
     */
    @Bean
    @ConditionalOnProperty(name = "jmouse.telegram.commands.menu", havingValue = "true", matchIfMissing = true)
    public ApplicationListener<ApplicationReadyEvent> telegramCommandMenu(
            CommandRouter router, BotMenus menus, TelegramSettings settings) {
        return event -> Thread.ofVirtual().name("jmouse-telegram-menu").start(() -> {
            try {
                menus.publish(settings.updates().purpose(), router.menu());
                LOGGER.info("telegram command menu published for '{}': {} commands",
                            settings.updates().purpose(), router.menu().size());
            } catch (RuntimeException exception) {
                LOGGER.warn("the telegram command menu could not be published; commands still work when typed",
                            exception);
            }
        });
    }
}
