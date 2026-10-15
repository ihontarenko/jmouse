package org.jmouse.telegram.user;

import org.jmouse.telegram.CallbackAnswer;
import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.SentMessage;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.spi.TelegramTransport;

import java.util.EnumSet;
import java.util.Set;

/**
 * The {@code USER} transport: sending as a person, through the gateway every product already calls.
 *
 * <p>A {@code USER} identity's credential is a <strong>session name</strong>, not a secret: the secret is
 * the session itself, on disk, encrypted by a key derived from the master key ({@link UserSessions}).
 *
 * <p>⚠️ No inline buttons and no callback answers — MTProto as a person has neither. A draft carrying
 * buttons is refused by {@code RoutingGateway} with {@code CapabilityUnavailable} naming {@code BOT} as the
 * kind that would have it.
 */
public final class UserTransport implements TelegramTransport {

    private static final Set<Capability> SUPPORTED = EnumSet.of(
            Capability.SEND_MESSAGE,
            Capability.EDIT_MESSAGE,
            Capability.DELETE_MESSAGE,
            Capability.SEND_MEDIA,
            Capability.INITIATE_CONVERSATION,
            Capability.READ_HISTORY,
            Capability.CREATE_CHAT);

    private final UserAccounts accounts;

    public UserTransport(UserAccounts accounts) {
        this.accounts = accounts;
    }

    @Override
    public IdentityKind kind() {
        return IdentityKind.USER;
    }

    @Override
    public boolean supports(Capability capability) {
        return SUPPORTED.contains(capability);
    }

    @Override
    public SentMessage send(TelegramIdentity identity, ChatReference chat, MessageDraft draft) {
        return accounts.ready(identity.credential()).send(chat, draft);
    }

    @Override
    public SentMessage edit(TelegramIdentity identity, MessageHandle message, MessageDraft draft) {
        return accounts.ready(identity.credential()).edit(message, draft);
    }

    @Override
    public void delete(TelegramIdentity identity, MessageHandle message) {
        accounts.ready(identity.credential()).delete(message);
    }

    @Override
    public void answerCallback(TelegramIdentity identity, CallbackAnswer answer) {
        // Unreachable through RoutingGateway, which checks ANSWER_CALLBACK first.
        throw new TelegramException(new TelegramRefusal.CapabilityUnavailable(
                Capability.ANSWER_CALLBACK, identity.name(), IdentityKind.USER, IdentityKind.BOT));
    }
}
