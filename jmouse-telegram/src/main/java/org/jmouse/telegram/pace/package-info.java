/**
 * How fast we are allowed to talk to Telegram, and what to do when it says we were too fast.
 *
 * <p>{@link org.jmouse.telegram.pace.PacedTransport} wraps any
 * {@link org.jmouse.telegram.spi.TelegramTransport}, so the pacing is composed in rather than built
 * into a protocol — the MTProto transport will need the same treatment, and an application with a
 * gateway already doing it can leave the decorator off.
 *
 * <p>⚠️ A {@link org.jmouse.telegram.pace.Pace} <em>waits</em> where a rate limiter would refuse. The
 * caller here is a message somebody is expecting: dropping it loses the message, and sending it anyway
 * earns a penalty that delays every other message too.
 */
package org.jmouse.telegram.pace;
