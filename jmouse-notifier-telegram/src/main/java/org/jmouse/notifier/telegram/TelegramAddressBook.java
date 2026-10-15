package org.jmouse.notifier.telegram;

import org.jmouse.notifier.Addressee;
import org.jmouse.telegram.ChatReference;

import java.util.List;

/**
 * Where a person is reachable on Telegram.
 *
 * <p>{@link BindingsAddressBook} answers from {@code jmouse-telegram-jpa}'s person bindings; a product
 * that keeps chat ids on its own accounts implements this in two lines instead.
 */
public interface TelegramAddressBook {

    /**
     * @return the chats that reach this person — none when they never bound one; never {@code null}
     */
    List<ChatReference> chatsOf(Addressee addressee);

    /**
     * Told when a chat can never be reached again — the person blocked the bot, or the chat is gone.
     *
     * <p>⚠️ <strong>Not optional in spirit.</strong> An address book that keeps answering with a blocked
     * chat produces a delivery that fails on every notification, forever.
     */
    default void unreachable(ChatReference chat) {
    }

    /** An address book that reaches nobody — for a notifier that only ever sends {@code Direct}. */
    static TelegramAddressBook nobody() {
        return addressee -> List.of();
    }
}
