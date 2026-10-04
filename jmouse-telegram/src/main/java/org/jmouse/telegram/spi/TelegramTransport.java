package org.jmouse.telegram.spi;

import org.jmouse.telegram.CallbackAnswer;
import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.SentMessage;
import org.jmouse.telegram.TelegramIdentity;

/**
 * One way of actually talking to Telegram.
 *
 * <h2>Why this exists beneath {@link org.jmouse.telegram.TelegramGateway}</h2>
 *
 * <p>The gateway is what a product calls: it knows about purposes, resolves an identity, and routes.
 * A transport is what speaks a protocol: it is handed the identity and does one thing with it.
 *
 * <p>⚠️ <strong>The identity is a parameter here, and that is the point of the split.</strong> Without
 * it, a routing gateway would resolve an identity to learn which transport to use and the transport
 * would resolve it again to use it — two resolutions per call, against a store, with a window between
 * them in which an administrator's edit lands and the call runs half under each version.
 *
 * <p>A transport is therefore stateless with respect to configuration, which also makes it the easy
 * half to write a fake of: no {@link org.jmouse.telegram.IdentitySource}, no purposes, no fallback
 * rules.
 *
 * <p>⚠️ Messaging only. Administering a chat is a separate SPI, for the reason given on the gateway:
 * the code that sends notifications and the code that bans members are never the same code.
 */
public interface TelegramTransport {

    /** Which of Telegram's two APIs this speaks, and therefore which identities it can be handed. */
    IdentityKind kind();

    /**
     * Whether this transport can do something at all.
     *
     * <p>⚠️ A property of the protocol, fixed at compile time — a Bot API transport will never create
     * a channel however it is configured. It is not a property of the identity's rights in a chat,
     * which only Telegram can answer and only when asked.
     */
    boolean supports(Capability capability);

    SentMessage send(TelegramIdentity identity, ChatReference chat, MessageDraft draft);

    SentMessage edit(TelegramIdentity identity, MessageHandle message, MessageDraft draft);

    void delete(TelegramIdentity identity, MessageHandle message);

    void answerCallback(TelegramIdentity identity, CallbackAnswer answer);
}
