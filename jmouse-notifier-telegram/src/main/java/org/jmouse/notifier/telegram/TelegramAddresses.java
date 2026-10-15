package org.jmouse.notifier.telegram;

import org.jmouse.notifier.Address;
import org.jmouse.telegram.ChatReference;

/**
 * How a Telegram chat is written as a notifier {@link Address} and read back.
 *
 * <p>Notation: the chat's wire value — a signed id such as {@code -1001234567890}, or {@code @username}
 * — optionally followed by {@code #<thread>} for a forum topic: {@code -1001234567890#7}.
 *
 * <p>⚠️ The value is stored by a delivery store, so this notation is a format, and changing it orphans
 * every pending delivery written in the old one.
 */
public final class TelegramAddresses {

    private static final char THREAD_SEPARATOR = '#';

    private TelegramAddresses() {
    }

    public static Address of(ChatReference chat) {
        return of(TelegramChannel.NAME, chat);
    }

    /** For a channel installed under a name other than {@link TelegramChannel#NAME}. */
    public static Address of(String channelName, ChatReference chat) {
        String value = chat.hasThread()
                ? chat.wireValue() + THREAD_SEPARATOR + chat.threadId()
                : chat.wireValue();

        return new Address(channelName, value);
    }

    /**
     * Reads an address back into a chat.
     *
     * @throws IllegalArgumentException when the value is neither a number nor a {@code @username} — a
     *                                  sentence rather than a {@link NumberFormatException}, because it
     *                                  is usually somebody's typing
     */
    public static ChatReference chatOf(Address address) {
        String value    = address.value().strip();
        int    position = value.lastIndexOf(THREAD_SEPARATOR);
        String chat     = position < 0 ? value : value.substring(0, position);

        ChatReference reference = ChatReference.parse(chat);

        if (position < 0) {
            return reference;
        }

        try {
            return reference.inThread(Integer.parseInt(value.substring(position + 1)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "'%s' names a topic that is not a number".formatted(address.value()), exception);
        }
    }

}
