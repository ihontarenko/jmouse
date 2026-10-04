/**
 * A transport that pretends, so a consumer can be built with no network and no bot.
 *
 * <p>⚠️ In {@code src/main/java} rather than a test artefact, deliberately: this repository keeps
 * integration checks as {@code smoke} classes with {@code main} methods, and a consuming product needs
 * {@link org.jmouse.telegram.test.RecordingTransport} on its own compile path to develop against.
 *
 * <p>Its value is not the happy path — it is that a flood wait, a blocked bot and an unchanged edit are
 * reachable on purpose here, and essentially unreachable against real Telegram.
 */
package org.jmouse.telegram.test;
