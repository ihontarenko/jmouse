package org.jmouse.notifier.telegram;

import org.jmouse.notifier.Addressee;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.jpa.TelegramBindings;

import java.util.List;

/**
 * An address book over the person bindings {@code jmouse-telegram-jpa} keeps — the deep-link flow's
 * result.
 *
 * <p>⚠️ Needs {@code jmouse-telegram-jpa} on the classpath; this module declares it optional.
 *
 * <p>⚠️ The addressee's subject must be the same identifier the product passed to
 * {@link TelegramBindings#invite(String)}. A product that binds by account and resolves by profile
 * reaches nobody, silently.
 */
public final class BindingsAddressBook implements TelegramAddressBook {

    private final TelegramBindings bindings;

    public BindingsAddressBook(TelegramBindings bindings) {
        this.bindings = bindings;
    }

    @Override
    public List<ChatReference> chatsOf(Addressee addressee) {
        return bindings.deliverableFor(addressee.subject()).stream()
                       .map(TelegramBindings.Binding::chat)
                       .toList();
    }

    @Override
    public void unreachable(ChatReference chat) {
        bindings.deactivate(chat.inChat());
    }
}
