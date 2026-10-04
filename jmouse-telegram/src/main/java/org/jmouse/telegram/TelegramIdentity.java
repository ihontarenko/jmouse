package org.jmouse.telegram;

import java.util.Objects;

/**
 * Who we speak to Telegram as.
 *
 * <p>A value type and nothing else — no annotations, no identity column, no timestamps. Where these
 * come from is {@link IdentitySource}'s business: a row an administrator edits, a properties file, a
 * secret manager, or three lines in a smoke test. This follows {@code ProviderSettings} deliberately,
 * and for the reason recorded there: the implementation that lesson came from made the settings a JPA
 * entity, which meant every consumer that merely wanted to send something also acquired a persistence
 * provider.
 *
 * <h2>⚠️ {@code name} is an addressable name, not a label</h2>
 *
 * <p>It is what a refusal quotes, what an administration screen lists, and what a journal row records.
 * A configuration with two bots and no way to say which one failed is a configuration nobody can debug,
 * so the name travels with every call rather than being held only by whoever resolved it.
 *
 * <h2>⚠️ {@code credential} is a secret and behaves like one</h2>
 *
 * <p>A bot token is complete control of the bot; a {@link IdentityKind#USER} session, when that
 * arrives, is complete control of a person's account. Hence {@link #toString()} is overridden to
 * redact it — a record's generated {@code toString} would otherwise put the token into the first log
 * line that ever prints an identity, and log files outlive the rotation that was supposed to fix it.
 *
 * @param kind       which of Telegram's two APIs this speaks
 * @param name       what this identity is called; quoted in refusals and journal entries
 * @param credential the bot token, or the session reference for a user account
 * @param apiBase    where to send it, or {@code null} for Telegram's own address. ⚠️ Not decoration:
 *                   a self-hosted {@code telegram-bot-api} server is the only way past the Bot API's
 *                   50 MB upload ceiling, and this field is how a deployment points at one
 */
public record TelegramIdentity(
        IdentityKind kind,
        String       name,
        String       credential,
        String       apiBase
) {

    /** Telegram's own Bot API address, used when {@link #apiBase} says nothing. */
    public static final String TELEGRAM_API_BASE = "https://api.telegram.org";

    public TelegramIdentity {
        Objects.requireNonNull(kind, "identity kind");
        Objects.requireNonNull(name, "identity name");
    }

    /** A bot at Telegram's own address — the ordinary case. */
    public static TelegramIdentity bot(String name, String token) {
        return new TelegramIdentity(IdentityKind.BOT, name, token, null);
    }

    /** A bot against a self-hosted Bot API server. */
    public static TelegramIdentity bot(String name, String token, String apiBase) {
        return new TelegramIdentity(IdentityKind.BOT, name, token, apiBase);
    }

    /** Where to send it: what was configured, or Telegram's own address. */
    public String apiBaseOrDefault() {
        if (apiBase == null || apiBase.isBlank()) {
            return TELEGRAM_API_BASE;
        }

        return apiBase;
    }

    public boolean hasCredential() {
        return credential != null && !credential.isBlank();
    }

    /**
     * ⚠️ Redacts the credential. See the class note — a record's generated {@code toString} is how a
     * token reaches a log file.
     */
    @Override
    public String toString() {
        return "TelegramIdentity[kind=%s, name=%s, credential=%s, apiBase=%s]".formatted(
                kind, name, hasCredential() ? "<redacted>" : "<absent>", apiBaseOrDefault());
    }
}
