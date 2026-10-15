package org.jmouse.telegram.menu;

import java.util.List;

/**
 * A bot's command menu — the list behind Telegram's "Menu" button.
 *
 * <p>⚠️ Bot configuration, not a message: it is set once for the bot and every chat sees it, which is why it
 * is neither on {@code TelegramGateway} (sending) nor on {@code ChatAdministration} (one chat).
 *
 * <p>⚠️ <strong>Publish what the bot actually handles.</strong> {@code TelegramCommandAutoConfiguration}
 * publishes the {@code CommandRouter}'s own commands at start-up, so the menu cannot advertise a command
 * the code no longer has — a hand-maintained list drifts the first time a command is renamed.
 */
public interface BotMenus {

    /**
     * Replaces the menu of the bot behind a purpose.
     *
     * @param languageCode a two-letter code for a menu in that language, or {@code null} for everybody else
     */
    void publish(String purpose, List<MenuEntry> entries, String languageCode);

    default void publish(String purpose, List<MenuEntry> entries) {
        publish(purpose, entries, null);
    }

    /** Removes the menu, so the button disappears. */
    void clear(String purpose, String languageCode);
}
