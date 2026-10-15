/**
 * The Telegram administration API — accounts, bindings, the journal, chats — shaped for a panel, with no
 * panel. Ivan, 2026-09-29: «Без юай».
 *
 * <h2>⚠️ Mounting — component scan does not reach it</h2>
 *
 * <pre>{@code
 * @Import(TelegramManagementConfiguration.class)
 * }</pre>
 *
 * <p>Nothing here is auto-configured. A library management module has to be mounted by the product, and
 * that has been learned the hard way in this repository before: a module that relied on scanning simply
 * never appeared, and the symptom was a 404 that looked like an empty installation.
 *
 * <h2>⚠️ No gate of its own</h2>
 *
 * <p>These controllers carry no authorization. The product mounts them behind its own rule — with
 * {@code jmouse-access}, an {@code ExternalAccessRules} bean gating these types on a permission. A product
 * that mounts them behind nothing has published who is bound to which chat, and a button that sends as
 * its bot.
 *
 * <h2>Routes — under {@link org.jmouse.telegram.management.ManagementRoutes#PREFIX}, default {@code /jmouse/telegram/api}</h2>
 *
 * <table>
 *   <tr><td>{@code GET /accounts} · {@code GET|PUT|DELETE /accounts/{purpose}}</td><td>accounts — ⚠️ the credential is never returned</td></tr>
 *   <tr><td>{@code PUT /accounts/{purpose}/credential} · {@code PUT /accounts/{purpose}/enabled}</td><td>rotate a token · switch on or off</td></tr>
 *   <tr><td>{@code POST /accounts/{purpose}/test-message}</td><td>send one message; a refusal comes back as data</td></tr>
 *   <tr><td>{@code GET /bindings} · {@code POST /bindings/invitations} · {@code DELETE /bindings}</td><td>who is bound where · start one · forget one</td></tr>
 *   <tr><td>{@code GET /journal}</td><td>every call, newest first, filterable</td></tr>
 *   <tr><td>{@code GET /chats/{chat}}</td><td>one chat as a purpose's bot sees it</td></tr>
 * </table>
 *
 * <p>⚠️ <strong>No outbox here.</strong> Delivery state belongs to {@code jmouse-notifier} (decided
 * 2026-10-06), shared by every channel.
 */
package org.jmouse.telegram.management;
