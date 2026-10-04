/**
 * Telegram's Bot API, over the JDK HTTP client.
 *
 * <p>{@link org.jmouse.telegram.bot.BotApiTransport} is the only public class worth knowing:
 * construct one, hand it to a {@link org.jmouse.telegram.RoutingGateway}, and everything else here is
 * its machinery. {@link org.jmouse.telegram.bot.BotApiClient} carries what every method call has in
 * common — timeouts, JSON, credentials, and the three ways a call fails — so that adding a method is
 * three small things and never a fourth copy of the error handling.
 *
 * <h2>⚠️ Where the prose of a Telegram error is read</h2>
 *
 * <p>{@code BotApiErrors}, and nowhere else. Telegram distinguishes a missing chat from an unchanged
 * edit from a blocked bot only in an English sentence, so matching on it is unavoidable — and
 * containing that match to one file is what stops the same fragile {@code contains("blocked")}
 * appearing in every consuming product.
 *
 * <h2>Dependencies</h2>
 *
 * <p>{@code jmouse-telegram} and Jackson, for the wire alone — nothing here binds to Jackson
 * annotations or its object model. The HTTP client is the JDK's, so there is no web stack.
 */
package org.jmouse.telegram.bot;
