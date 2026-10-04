package org.jmouse.telegram;

/**
 * Something a transport can or cannot do, asked before it is attempted.
 *
 * <h2>⚠️ Why this exists at all</h2>
 *
 * <p>Two transports sit behind one {@link TelegramGateway}, and they are <strong>not</strong>
 * equivalent. A bot cannot create a group or a channel — that is MTProto only. A user account cannot
 * put an inline keyboard under a message or answer a callback query — those are bot concepts and the
 * user API has no equivalent. Neither gap is an oversight anybody can close.
 *
 * <p>One interface in front of genuinely different powers is exactly the arrangement where pretending
 * parity produces the worst available failure: code that compiles, passes review, ships, and then
 * throws {@code UnsupportedOperationException} at a customer with no sentence attached. So the
 * difference is data rather than an accident — {@link TelegramGateway#supports(Capability)} is
 * askable in advance, and attempting one anyway produces
 * {@link TelegramRefusal.CapabilityUnavailable}, which names the identity, the capability and the
 * kind of transport that would have had it.
 *
 * <p>⚠️ This enum describes <em>transport</em> power, never <em>permission</em>. That a bot is
 * technically able to ban a member says nothing about whether it is an administrator of that chat, and
 * nothing at all about whether the person driving it is allowed to. Telegram answers the first at call
 * time; the second belongs to the product and to {@code jmouse-access}.
 */
public enum Capability {

    /** Send a message. Every transport has this; it is listed so a roster is complete rather than partial. */
    SEND_MESSAGE,

    /** Edit a message already sent — the difference between a useful channel and a spam source. */
    EDIT_MESSAGE,

    /** Delete a message already sent. */
    DELETE_MESSAGE,

    /** Attach a photo, document, video, audio, or a media group. */
    SEND_MEDIA,

    /** ⚠️ Inline keyboards and reply keyboards. Bot API only — the user API has no concept of one. */
    REPLY_MARKUP,

    /** ⚠️ Acknowledge a button press. Bot API only, and meaningless without {@link #REPLY_MARKUP}. */
    ANSWER_CALLBACK,

    /** Administer a chat the identity is already in: members, rights, bans, invite links, settings. */
    ADMINISTER_CHAT,

    /** Create and manage forum topics inside a supergroup. */
    MANAGE_TOPICS,

    /**
     * ⚠️ Create a group or a channel. <strong>MTProto only</strong> — it is absent from the Bot API
     * entirely, which is the single most commonly assumed-present capability in this list and the
     * reason the enum exists.
     */
    CREATE_CHAT,

    /**
     * ⚠️ Write to somebody who has not written first. A bot may not: a person has to start it, which
     * is precisely why person-to-chat binding is a flow rather than a configuration value.
     */
    INITIATE_CONVERSATION,

    /** Read a chat's earlier messages. Not available to a bot, which only ever sees what arrives. */
    READ_HISTORY
}
