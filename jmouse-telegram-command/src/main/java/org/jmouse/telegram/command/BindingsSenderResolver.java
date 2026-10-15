package org.jmouse.telegram.command;

import org.jmouse.telegram.jpa.TelegramBindings;
import org.jmouse.telegram.update.TelegramUser;

import java.util.Optional;

/**
 * Senders resolved through the person bindings {@code jmouse-telegram-jpa} keeps.
 *
 * <p>⚠️ Needs {@code jmouse-telegram-jpa} on the classpath; this module declares it optional. Keyed on the
 * sender's numeric id, never the username.
 */
public final class BindingsSenderResolver implements SenderResolver {

    private final TelegramBindings bindings;

    public BindingsSenderResolver(TelegramBindings bindings) {
        this.bindings = bindings;
    }

    @Override
    public Optional<String> subjectOf(TelegramUser sender) {
        if (sender == null || sender.bot()) {
            return Optional.empty();
        }

        return bindings.subjectOf(sender.id());
    }
}
