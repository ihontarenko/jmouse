/**
 * Telegram, as a contract rather than as a client library.
 *
 * <h2>What is here</h2>
 *
 * <p>{@link org.jmouse.telegram.TelegramGateway} is the one interface a product calls. Behind it,
 * {@link org.jmouse.telegram.RoutingGateway} resolves a {@link org.jmouse.telegram.TelegramIdentity}
 * from an {@link org.jmouse.telegram.IdentitySource}, picks the
 * {@link org.jmouse.telegram.spi.TelegramTransport} that speaks its protocol, and checks the
 * {@link org.jmouse.telegram.Capability Capabilities} the call needs before making it.
 *
 * <pre>{@code
 * SentMessage sent = gateway.as("kitsu-notifications")
 *         .send(ChatReference.of(chatId), MessageDraft.text("Blade Runner was opened"));
 * }</pre>
 *
 * <h2>⚠️ Telegram is two APIs, and this package is shaped around that</h2>
 *
 * <p>The Bot API and MTProto are different protocols with genuinely different powers — a bot cannot
 * create a group, a user account cannot put a button under a message. Rather than two unrelated client
 * classes, there is one contract, an {@link org.jmouse.telegram.IdentityKind} on the identity, and a
 * {@link org.jmouse.telegram.Capability} roster that makes the difference answerable in advance and
 * refusable with a sentence when it is not.
 *
 * <h2>⚠️ This is not a notification framework</h2>
 *
 * <p>It carries a message and says what happened. <em>What is worth telling somebody about</em> is a
 * product's question, and deliberately not this library's: a consumer that also notifies by web push
 * or in an application needs a channel abstraction above this one, and a Telegram module that owned
 * the word "notification" would force every other channel to be built around a Telegram-shaped hole.
 *
 * <h2>Dependencies</h2>
 *
 * <p>{@code jmouse-core} and {@code jmouse-http}, and nothing else. No Jackson — the wire format is a
 * transport's business. No Spring — an application that wants to send a message should not acquire a
 * container to do it. No persistence — an identity is a value type here, and where identities are kept
 * belongs to {@code jmouse-telegram-jpa} or to a properties file.
 */
package org.jmouse.telegram;
