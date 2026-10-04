/**
 * Telegram in a Spring Boot application: add the dependency, set a token, inject a
 * {@link org.jmouse.telegram.TelegramGateway}.
 *
 * <p>{@link org.jmouse.telegram.spring.TelegramSettings} is the whole configuration surface, and
 * ⚠️ its keys are a published contract — every adopting product writes them into its own
 * configuration, so renaming one breaks each of them at once.
 *
 * <p>⚠️ Nothing is started that was not asked for. {@code jmouse.telegram.updates.mode} defaults to
 * {@code none}, so an application that only sends acquires neither a polling thread nor a public
 * route.
 */
package org.jmouse.telegram.spring;
