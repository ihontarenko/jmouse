package org.jmouse.telegram;

/**
 * Which of Telegram's two APIs an identity speaks.
 *
 * <p>⚠️ <strong>This is not a detail of configuration; it is the largest fact in this library.</strong>
 * Telegram is two protocols, not one endpoint with two credential types, and the powers on either side
 * genuinely differ: a bot cannot create a group, and a user account cannot answer a callback query.
 * Modelling that as an enum on the identity — rather than as two unrelated client classes — is what
 * lets one {@link TelegramGateway} contract stand in front of both while {@link Capability} keeps it
 * honest about the difference.
 *
 * @see Capability
 * @see RoutingGateway
 */
public enum IdentityKind {

    /**
     * A bot, over the HTTP Bot API, authenticated by a token.
     *
     * <p>The ordinary case: cheap, supported, within Telegram's terms, and the only one that can put a
     * button under a message. Its limits are real though — a bot may not write to somebody who has
     * never started it, and it administers only the chats it was added to as an administrator.
     */
    BOT,

    /**
     * A person's own account, over MTProto.
     *
     * <p>⚠️ <strong>No transport implements this yet</strong> — the constant exists so that the
     * contract, the routing and the capability checks are built around both kinds from the first
     * commit rather than retrofitted around one. An {@link IdentitySource} may legitimately answer
     * with a {@code USER} identity today; {@link RoutingGateway} will refuse it with a sentence
     * saying no transport is installed, which is the honest answer and not a defect.
     *
     * <p>⚠️ Automating a user account is against Telegram's terms of service and accounts running one
     * do get limited. That is a decision to take deliberately, not to discover.
     */
    USER
}
