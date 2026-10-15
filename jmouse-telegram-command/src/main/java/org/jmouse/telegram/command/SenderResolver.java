package org.jmouse.telegram.command;

import org.jmouse.telegram.update.TelegramUser;

import java.util.Optional;

/**
 * Who a Telegram sender is in the product.
 *
 * <p>⚠️ <strong>This is the line where a Telegram account becomes a credential.</strong> Answer only from a
 * binding the person made from an authenticated session in the product — the deep-link flow — and never
 * from anything the message says about itself: a username can be changed by anybody to anything.
 *
 * <p>{@link BindingsSenderResolver} answers from {@code jmouse-telegram-jpa}'s bindings.
 */
@FunctionalInterface
public interface SenderResolver {

    /**
     * @return the product subject this sender acts as, or empty when they are not bound — and then no
     *         handler runs
     */
    Optional<String> subjectOf(TelegramUser sender);
}
