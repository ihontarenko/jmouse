package org.jmouse.telegram;

/**
 * Carrying a message to Telegram, and saying what happened.
 *
 * <p>The one contract every consumer sees. Two transports implement it — the Bot API over HTTP, and
 * later a user account over MTProto — and a caller distinguishes them only by asking
 * {@link #supports(Capability)} or by reading a refusal.
 *
 * <h2>⚠️ It carries messages and nothing else</h2>
 *
 * <p>Administering a chat — members, rights, bans, invite links, topics — is a separate interface, not
 * eleven more methods here. The two are used by different code at different times: a notification
 * sender never bans anybody, and an administration screen never composes a draft. One interface for
 * both would force every implementation, including a fake one written for a product's tests, to answer
 * for work it has no part in.
 *
 * <h2>⚠️ The identity is resolved per call, not held</h2>
 *
 * <p>There is no {@code identity()} accessor. A gateway asks its {@link IdentitySource} as each call is
 * made, so an administrator who rotates a token or disables an account sees the next send obey it — see
 * {@link IdentitySource} for why a restart is the wrong answer to a leaked credential.
 *
 * <p>Which identity is used comes from a <em>purpose</em>. {@link #as(String)} binds one, and the bare
 * methods mean {@link IdentitySource#GENERAL}:
 *
 * <pre>{@code
 * gateway.as("kitsu-notifications").send(chat, MessageDraft.text("Blade Runner was opened"));
 * }</pre>
 *
 * <h2>Failure</h2>
 *
 * <p>Every method throws {@link TelegramException}, whose {@link TelegramRefusal} is the part worth
 * switching on. Nothing here returns {@code null} to mean failure and nothing returns a boolean: a
 * send that did not happen has a reason, and the reason decides whether an outbox retries it, drops it,
 * or deactivates a binding.
 */
public interface TelegramGateway {

    /**
     * Sends a message.
     *
     * <p>⚠️ Returns the {@link SentMessage} rather than nothing. That handle is the only way to edit
     * or delete it later, and editing is how a channel reports a changing fact without posting the
     * same notice three times.
     *
     * @throws TelegramException when Telegram refused, nothing answered, or the resolved identity
     *                           lacks a capability the draft needs
     */
    SentMessage send(ChatReference chat, MessageDraft draft);

    /**
     * Replaces the content of a message already sent.
     *
     * <p>⚠️ An edit that changes nothing is refused by Telegram, and arrives as
     * {@link TelegramRefusal.MessageNotModified}. It is benign — a periodic updater recomputing the
     * same text hits it every time — and treating it as an error is how such a job logs an exception a
     * minute forever.
     */
    SentMessage edit(MessageHandle message, MessageDraft draft);

    /**
     * Deletes a message.
     *
     * <p>⚠️ Telegram allows this only within 48 hours for an ordinary message, and the refusal for an
     * older one does not say so. Editing is the durable way to retract something.
     */
    void delete(MessageHandle message);

    /**
     * Answers a button press.
     *
     * <p>⚠️ Not optional — see {@link CallbackAnswer}. Needs {@link Capability#ANSWER_CALLBACK}.
     */
    void answerCallback(CallbackAnswer answer);

    /**
     * Whether this gateway, for the identity it would resolve, can do something at all.
     *
     * <p>⚠️ About the transport's power, never about permission. That a bot <em>can</em> ban a member
     * says nothing about whether it administers that chat, and nothing at all about whether the person
     * driving it is allowed to — the first is Telegram's answer at call time, the second belongs to the
     * product.
     */
    boolean supports(Capability capability);

    /**
     * The same gateway, speaking as whichever identity is configured for {@code purpose}.
     *
     * <p>The purpose is the product's own word and this library never enumerates one. An unknown
     * purpose is not an error: {@link IdentitySource} falls back to
     * {@link IdentitySource#GENERAL}, so a product that has not configured a dedicated identity still
     * sends.
     */
    TelegramGateway as(String purpose);
}
