package org.jmouse.telegram.spring;

import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.IdentitySource;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything a product configures under {@code jmouse.telegram}.
 *
 * <pre>{@code
 * jmouse:
 *   telegram:
 *     identities:
 *       general:                       # the fallback purpose; see IdentitySource.GENERAL
 *         kind: bot
 *         token: ${TELEGRAM_BOT_TOKEN}
 *       notifications:                 # a purpose the product names itself — here, the shared bot
 *         kind: bot
 *         token: ${SHARED_NOTIFICATION_BOT_TOKEN}
 *       kitsu-commands:                # and the product's own bot, whose updates it consumes
 *         kind: bot
 *         token: ${KITSU_BOT_TOKEN}
 *     updates:
 *       mode: polling                  # none | polling | webhook
 *       purpose: kitsu-commands        # whose updates this application consumes; default general
 *       poll-timeout: 50s
 *       allowed: [message, callback_query]
 *       webhook:
 *         path: /telegram/webhook
 *         secret: ${TELEGRAM_WEBHOOK_SECRET}
 *     pace:
 *       enabled: true
 * }</pre>
 *
 * <p>⚠️ <strong>These keys are a published contract.</strong> Every adopting product writes them into
 * its own configuration, so renaming one is a break in each of them at once — which is why this record
 * exists at all rather than properties being read wherever they are needed.
 *
 * <p>⚠️ A token belongs in the vault, never in a committed file: {@code token: vault:telegram/notifications/token}
 * is resolved through {@code jmouse-vault} at each use, so a rotated token needs no restart. A literal
 * still works for an application without a vault.
 *
 * @param identities purpose → identity. The key is the <em>purpose</em>, which is the product's own
 *                   word; {@code general} is the one this library names, because it is the fallback
 * @param updates    how updates arrive, if at all
 * @param pace       how fast this application may talk to Telegram
 */
public record TelegramSettings(
        Map<String, Identity> identities,
        Updates               updates,
        Pacing                pace,
        Journal               journal
) {

    public TelegramSettings {
        identities = identities == null ? Map.of() : new LinkedHashMap<>(identities);
        updates    = updates == null ? Updates.none() : updates;
        pace       = pace == null ? Pacing.defaults() : pace;
        journal    = journal == null ? Journal.defaults() : journal;
    }

    /**
     * Where the journal of calls is kept, and for how long.
     *
     * @param store     {@code memory} (default) or {@code database} — the latter needs
     *                  {@code jmouse-telegram-jpa} and writes to {@code telegram_journal}
     * @param capacity  how many entries the memory store keeps
     * @param retention how long the database store keeps an entry; pruned hourly. ⚠️ A journal is a
     *                  diagnostic, not an archive — thirty days answers every "why did it not arrive"
     */
    public record Journal(JournalStore store, int capacity, Duration retention) {

        public static Journal defaults() {
            return new Journal(null, 0, null);
        }

        public Journal {
            store     = store == null ? JournalStore.MEMORY : store;
            capacity  = capacity <= 0 ? 1000 : capacity;
            retention = retention == null || retention.isNegative() || retention.isZero()
                    ? Duration.ofDays(30) : retention;
        }
    }

    /** Where journal entries go. */
    public enum JournalStore {

        /** The most recent entries in this process's memory. Forgotten on restart. */
        MEMORY,

        /** {@code telegram_journal}, through {@code jmouse-telegram-jpa}. */
        DATABASE,

        /** Nowhere — an application that has decided it wants no journal. */
        NONE
    }

    /**
     * One configured identity.
     *
     * @param kind    {@code bot} or {@code user}
     * @param token   the bot token, or a user session reference
     * @param apiBase a self-hosted {@code telegram-bot-api} server, or empty for Telegram's own.
     *                ⚠️ The only way past the 50 MB upload ceiling
     * @param enabled so an identity can be switched off without deleting its configuration. ⚠️ True when
     *                omitted — a primitive left to the binder would be false, and every call would refuse
     */
    public record Identity(IdentityKind kind, String token, String apiBase, @DefaultValue("true") boolean enabled) {

        public Identity {
            kind = kind == null ? IdentityKind.BOT : kind;
        }

        /** ⚠️ Redacts the token — see {@code TelegramIdentity.toString()} for why. */
        @Override
        public String toString() {
            return "Identity[kind=%s, token=%s, apiBase=%s, enabled=%s]".formatted(
                    kind, token == null || token.isBlank() ? "<absent>" : "<redacted>", apiBase, enabled);
        }
    }

    /**
     * @param mode        which ingestion runs. ⚠️ {@code none} is the default and it matters: an
     *                    application that only <em>sends</em> should not open a listener or a polling
     *                    thread it never asked for
     * @param pollTimeout how long Telegram holds an idle {@code getUpdates} open
     * @param allowed     which update kinds to receive, or empty for Telegram's default. ⚠️ That
     *                    default excludes {@code chat_member}, so an application that wants to know
     *                    who joined must name it here
     * @param webhook     the endpoint, when {@link Mode#WEBHOOK}
     * @param purpose     whose updates this application consumes — the identity polled. Default
     *                    {@code general}. ⚠️ <strong>Sending and receiving are different bots on
     *                    purpose.</strong> Telegram hands a bot's updates to exactly one consumer, so
     *                    several products can send through one shared bot but each must receive through
     *                    its own: name the product's bot here and the shared one under a sending purpose
     */
    public record Updates(Mode mode, Duration pollTimeout, List<String> allowed, Webhook webhook, String purpose) {

        public Updates {
            mode        = mode == null ? Mode.NONE : mode;
            pollTimeout = pollTimeout == null ? Duration.ofSeconds(50) : pollTimeout;
            allowed     = allowed == null ? List.of() : List.copyOf(allowed);
            webhook     = webhook == null ? new Webhook(null, null) : webhook;
            purpose     = purpose == null || purpose.isBlank() ? IdentitySource.GENERAL : purpose.strip();
        }

        public static Updates none() {
            return new Updates(Mode.NONE, null, null, null, null);
        }
    }

    /** How updates reach the application. */
    public enum Mode {

        /** Neither a listener nor a loop. The default, and correct for an application that only sends. */
        NONE,

        /**
         * A {@code getUpdates} loop.
         *
         * <p>⚠️ What development runs on here: a webhook needs a public HTTPS address, and this
         * machine serves plain HTTP from behind a router.
         */
        POLLING,

        /** Telegram posts to us. Production, and it needs a publicly reachable HTTPS address. */
        WEBHOOK
    }

    /**
     * @param path   where the endpoint listens
     * @param secret ⚠️ <strong>Not optional in practice.</strong> Telegram echoes it in
     *               {@code X-Telegram-Bot-Api-Secret-Token}, and without checking it the endpoint is a
     *               public route that accepts anything shaped like an update — meaning anybody can
     *               make the bot believe a message arrived
     */
    public record Webhook(String path, String secret) {

        /**
         * ⚠️ Under {@code /jmouse}, not {@code /telegram}, for the reason
         * {@link org.jmouse.core.management.ManagementEndpoints} gives: a library that publishes a
         * controller is publishing into somebody else's URL space, and a plausible-looking top-level
         * path is the one a product is most likely to have taken already. Two controllers on one path
         * is an ambiguous mapping and the context refuses to start.
         *
         * <p>It is still configurable and usually configured — the address is handed to Telegram in
         * {@code setWebhook}, so the product decides it either way.
         */
        public static final String DEFAULT_PATH =
                org.jmouse.core.management.ManagementEndpoints.DEFAULT_ROOT + "/telegram/webhook";

        public Webhook {
            path = path == null || path.isBlank() ? DEFAULT_PATH : path;
        }

        public boolean hasSecret() {
            return secret != null && !secret.isBlank();
        }

        @Override
        public String toString() {
            return "Webhook[path=%s, secret=%s]".formatted(path, hasSecret() ? "<redacted>" : "<absent>");
        }
    }

    /**
     * @param enabled         whether to pace at all. On by default: the failure it prevents is a bot
     *                        being rate-limited, which delays every message rather than one
     * @param globalPerSecond Telegram's published overall guidance
     * @param perChatPerMinute Telegram's published per-group guidance
     */
    public record Pacing(@DefaultValue("true") boolean enabled, int globalPerSecond, int perChatPerMinute) {

        public static Pacing defaults() {
            return new Pacing(true, 30, 20);
        }

        public Pacing {
            globalPerSecond   = globalPerSecond <= 0 ? 30 : globalPerSecond;
            perChatPerMinute  = perChatPerMinute <= 0 ? 20 : perChatPerMinute;
        }
    }
}
