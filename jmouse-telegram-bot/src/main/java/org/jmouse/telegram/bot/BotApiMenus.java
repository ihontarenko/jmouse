package org.jmouse.telegram.bot;

import org.jmouse.telegram.Capability;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.menu.BotMenus;
import org.jmouse.telegram.menu.MenuEntry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The command menu over the Bot API — {@code setMyCommands}, {@code deleteMyCommands}, and the chat menu
 * button set to show them.
 *
 * <p>⚠️ A {@code USER} identity has no menu; it is refused naming {@code BOT}.
 */
public class BotApiMenus extends BotApiClient implements BotMenus {

    private final IdentitySource identities;

    public BotApiMenus(IdentitySource identities) {
        super();
        this.identities = Objects.requireNonNull(identities, "identity source");
    }

    public BotApiMenus(IdentitySource identities, Duration connectTimeout, Duration readTimeout) {
        super(connectTimeout, readTimeout);
        this.identities = Objects.requireNonNull(identities, "identity source");
    }

    @Override
    public void publish(String purpose, List<MenuEntry> entries, String languageCode) {
        TelegramIdentity identity = bot(purpose);

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("commands", entries.stream()
                                      .map(entry -> Map.of("command", entry.command(), "description", entry.description()))
                                      .toList());
        putLanguage(fields, languageCode);
        call(identity, "setMyCommands", fields);

        // ⚠️ The button itself: without it some clients show the list only when somebody types "/".
        Map<String, Object> button = new LinkedHashMap<>();
        button.put("menu_button", Map.of("type", "commands"));
        call(identity, "setChatMenuButton", button);
    }

    @Override
    public void clear(String purpose, String languageCode) {
        Map<String, Object> fields = new LinkedHashMap<>();
        putLanguage(fields, languageCode);
        call(bot(purpose), "deleteMyCommands", fields);
    }

    private TelegramIdentity bot(String purpose) {
        TelegramIdentity identity = identities.require(purpose);

        if (identity.kind() != IdentityKind.BOT) {
            throw new TelegramException(new TelegramRefusal.CapabilityUnavailable(
                    Capability.REPLY_MARKUP, identity.name(), identity.kind(), IdentityKind.BOT));
        }

        return identity;
    }

    private static void putLanguage(Map<String, Object> fields, String languageCode) {
        if (languageCode != null && !languageCode.isBlank()) {
            fields.put("language_code", languageCode);
        }
    }
}
