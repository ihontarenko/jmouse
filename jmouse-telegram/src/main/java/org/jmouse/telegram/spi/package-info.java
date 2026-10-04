/**
 * The seams a transport implements.
 *
 * <p>{@link org.jmouse.telegram.spi.TelegramTransport} is one way of actually talking to Telegram. It
 * is handed a {@link org.jmouse.telegram.TelegramIdentity} rather than resolving one, which is what
 * keeps purposes, fallbacks and configuration entirely in the gateway above it — and what makes a
 * fake transport a small class rather than a second implementation of the whole library.
 */
package org.jmouse.telegram.spi;
