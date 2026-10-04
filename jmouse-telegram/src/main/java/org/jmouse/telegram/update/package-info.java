/**
 * The inbound half: what arrived, and who wants it.
 *
 * <p>{@link org.jmouse.telegram.update.Update} is a sealed hierarchy with an
 * {@link org.jmouse.telegram.update.Update.Unknown} member — ⚠️ because Telegram's set of update types
 * is <em>not</em> closed. It grows, and a hierarchy with no room for an unrecognised one would throw
 * inside an ingestion loop on the day of a Telegram release, over an update nobody wanted.
 *
 * <p>{@link org.jmouse.telegram.update.UpdateDispatcher} is the fan-out, and that is architecture
 * rather than convenience: Telegram delivers a bot's updates to exactly one consumer, so two products
 * wanting the same bot cannot each have a listener — they share one ingress and register here.
 *
 * <p>Parsing belongs to a transport; this package knows nothing about JSON.
 */
package org.jmouse.telegram.update;
