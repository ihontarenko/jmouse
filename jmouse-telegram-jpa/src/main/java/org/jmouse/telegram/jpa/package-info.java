/**
 * Telegram accounts as rows, so a token can be rotated without a deploy.
 *
 * <p>{@link org.jmouse.telegram.jpa.JpaIdentitySource} is why the table exists: an administrator
 * rotates a credential, disables an account during an incident, or adds a purpose, and the next send
 * obeys it. {@link org.jmouse.telegram.jpa.TelegramAccounts} is how it is administered.
 *
 * <h2>⚠️ The credential never travels in the clear</h2>
 *
 * <p>It is sealed on the way in by {@link org.jmouse.telegram.jpa.CredentialCipher}, and opened in
 * exactly one place — at the moment of use. No read answers with it: {@code Description} has no
 * credential field at all, which is what lets an administration endpoint list accounts without a
 * reviewer having to check whether it masked something.
 *
 * <p>⚠️ There is deliberately <strong>no plaintext cipher</strong>. A pass-through default would be the
 * state an installation reaches by forgetting to configure a key.
 *
 * <h2>⚠️ It migrates itself, append-only</h2>
 *
 * <p>Own history table, own locations per dialect — see
 * {@link org.jmouse.telegram.jpa.migration.TelegramMigrations}. The workspace rule that Flyway files
 * may be edited in place applies to a product whose database can be dropped, never to a library other
 * people's data has already run.
 *
 * <h2>Dependencies</h2>
 *
 * <p>Jakarta Persistence only. No Spring Data and no Spring: transaction demarcation belongs to whoever
 * calls the store.
 */
package org.jmouse.telegram.jpa;
