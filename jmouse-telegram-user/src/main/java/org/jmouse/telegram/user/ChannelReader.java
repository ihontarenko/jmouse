package org.jmouse.telegram.user;

import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;

import java.util.Objects;

/**
 * Reading channels and groups as a person — what a bot cannot do at all: join by itself, or read anything
 * posted before it arrived.
 *
 * <pre>{@code
 * ChannelReader reader = new ChannelReader(identities, accounts).as("ivan-reader");
 * ChatReference channel = reader.join("https://t.me/+private-invite");
 * HistoryPage   page    = reader.history(channel, null, 100);
 * }</pre>
 *
 * <p>Live posts arrive through {@link UserAccounts#connect} into the same dispatcher a bot feeds, so
 * selecting "by criteria" is an ordinary {@code dispatcher.on(...)} predicate.
 *
 * <p>⚠️ A purpose resolving to a {@code BOT} identity is refused with {@code CapabilityUnavailable}
 * ({@link Capability#READ_HISTORY}), naming {@code USER} as the kind that has it.
 */
public final class ChannelReader {

    private final IdentitySource identities;
    private final UserAccounts   accounts;
    private final String         purpose;

    public ChannelReader(IdentitySource identities, UserAccounts accounts) {
        this(identities, accounts, IdentitySource.GENERAL);
    }

    private ChannelReader(IdentitySource identities, UserAccounts accounts, String purpose) {
        this.identities = Objects.requireNonNull(identities, "identity source");
        this.accounts   = Objects.requireNonNull(accounts, "accounts");
        this.purpose    = purpose;
    }

    public ChannelReader as(String purpose) {
        return new ChannelReader(identities, accounts, Objects.requireNonNull(purpose, "purpose"));
    }

    public ChatReference resolve(String username) {
        return client().resolve(username);
    }

    /** @param target a {@code @username} or an invite link */
    public ChatReference join(String target) {
        return client().join(target);
    }

    /**
     * @param olderThan the previous page's {@link HistoryPage#olderThan()}, or {@code null} for the newest
     * @param limit     at most 100 — Telegram's own page ceiling
     */
    public HistoryPage history(ChatReference chat, Integer olderThan, int limit) {
        return client().history(chat, olderThan, Math.clamp(limit, 1, 100));
    }

    private UserClient client() {
        TelegramIdentity identity = identities.require(purpose);

        if (identity.kind() != IdentityKind.USER) {
            throw new TelegramException(new TelegramRefusal.CapabilityUnavailable(
                    Capability.READ_HISTORY, identity.name(), identity.kind(), IdentityKind.USER));
        }

        return accounts.ready(identity.credential());
    }
}
