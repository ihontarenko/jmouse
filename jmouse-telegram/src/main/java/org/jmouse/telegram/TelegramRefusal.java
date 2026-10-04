package org.jmouse.telegram;

import java.time.Duration;

/**
 * Why a call did not happen, as a value rather than as prose.
 *
 * <h2>⚠️ Telegram answers with unstructured English, and that is the problem this closes</h2>
 *
 * <p>A refused Bot API call comes back as {@code {"ok":false,"error_code":403,
 * "description":"Forbidden: bot was blocked by the user"}}. The description is a sentence written for
 * a human, it is not a stable identifier, and it has been reworded before. Left untranslated, every
 * caller in every product writes its own {@code description.contains("blocked")} — each one slightly
 * different, all of them silently wrong the day the wording changes.
 *
 * <p>So the translation happens exactly once, in the transport, and callers switch on a type. The
 * cases below are the ones with <em>different consequences</em>, which is the only reason to
 * distinguish a failure at all:
 *
 * <table>
 *   <caption>What each one means the caller should do</caption>
 *   <tr><th>Refusal</th><th>Consequence</th></tr>
 *   <tr><td>{@link FloodWait}</td><td>wait exactly this long, then retry</td></tr>
 *   <tr><td>{@link BotBlocked}</td><td>⚠️ stop sending to this person and deactivate the binding</td></tr>
 *   <tr><td>{@link ChatNotFound}</td><td>the destination is wrong; retrying cannot fix it</td></tr>
 *   <tr><td>{@link MessageNotModified}</td><td>benign — the edit was a no-op</td></tr>
 *   <tr><td>{@link Unauthorized}</td><td>the credential is wrong or revoked; an administrator must act</td></tr>
 *   <tr><td>{@link InsufficientRights}</td><td>the identity is in the chat but not an administrator of it</td></tr>
 *   <tr><td>{@link CapabilityUnavailable}</td><td>this transport cannot do this at all; a different identity kind could</td></tr>
 *   <tr><td>{@link Rejected}</td><td>Telegram said no for a reason we do not model</td></tr>
 *   <tr><td>{@link TransportFailure}</td><td>nothing answered; retryable</td></tr>
 * </table>
 *
 * <p>⚠️ Sealed, so a caller that switches over these gets a compiler error when a case is added rather
 * than falling into a default branch that does the wrong thing quietly.
 */
public sealed interface TelegramRefusal {

    /** One sentence somebody can act on. Never a stack trace, never Telegram's raw JSON. */
    String message();

    /**
     * Whether trying the identical call again could succeed.
     *
     * <p>⚠️ The distinction is the whole value of this taxonomy to an outbox: retrying a
     * {@link ChatNotFound} forever fills a queue with work that can never succeed, and <em>not</em>
     * retrying a {@link TransportFailure} loses a message to a thirty-second outage.
     */
    boolean retryable();

    /**
     * Telegram is rate-limiting us and has said for how long.
     *
     * <p>⚠️ <strong>Obey {@link #retryAfter} rather than inventing a backoff.</strong> Telegram
     * already stated the figure; a shorter guess is refused again and extends the penalty, and a
     * longer one wastes the delivery window for no benefit.
     */
    record FloodWait(Duration retryAfter) implements TelegramRefusal {

        /**
         * ⚠️ Reports milliseconds below a second. Telegram's own figure is whole seconds, but a pace
         * policy and a test both construct sub-second waits — and "retry after 0 second(s)", which is
         * what rounding produced, reads as a bug in the retry rather than as a short delay.
         */
        @Override
        public String message() {
            if (retryAfter.toMillis() < 1000) {
                return "Telegram is rate-limiting this identity; retry after %dms"
                        .formatted(retryAfter.toMillis());
            }

            return "Telegram is rate-limiting this identity; retry after %d second(s)"
                    .formatted(retryAfter.toSeconds());
        }

        @Override
        public boolean retryable() {
            return true;
        }
    }

    /**
     * The person turned the bot off.
     *
     * <p>⚠️ <strong>This is an answer, not a failure to retry past.</strong> It is the one refusal
     * with a bookkeeping consequence: the binding that says this person can be reached is no longer
     * true, and leaving it active means an outbox that accumulates work which can never be delivered
     * and never be cleared.
     */
    record BotBlocked(String chat) implements TelegramRefusal {

        @Override
        public String message() {
            return "the bot was blocked by the user in chat %s; the binding is no longer deliverable"
                    .formatted(chat);
        }

        @Override
        public boolean retryable() {
            return false;
        }
    }

    /** The destination does not exist, or this identity cannot see it. */
    record ChatNotFound(String chat) implements TelegramRefusal {

        @Override
        public String message() {
            return "chat %s was not found by this identity".formatted(chat);
        }

        @Override
        public boolean retryable() {
            return false;
        }
    }

    /**
     * An edit that changed nothing.
     *
     * <p>Benign and common: a periodic edit that recomputes the same text hits it every time. It is
     * modelled rather than swallowed because a caller may reasonably want to know the edit was a
     * no-op — but treating it as an error is how a scheduled updater ends up logging an exception a
     * minute forever.
     */
    record MessageNotModified() implements TelegramRefusal {

        @Override
        public String message() {
            return "the message already had this content; nothing was changed";
        }

        @Override
        public boolean retryable() {
            return false;
        }
    }

    /** The credential is absent, wrong, or revoked. */
    record Unauthorized(String identityName, String detail) implements TelegramRefusal {

        @Override
        public String message() {
            return "identity '%s' was rejected by Telegram: %s".formatted(identityName, detail);
        }

        @Override
        public boolean retryable() {
            return false;
        }
    }

    /** The identity is in the chat but lacks the administrator right this call needs. */
    record InsufficientRights(String chat, String detail) implements TelegramRefusal {

        @Override
        public String message() {
            return "insufficient rights in chat %s: %s".formatted(chat, detail);
        }

        @Override
        public boolean retryable() {
            return false;
        }
    }

    /**
     * This transport cannot do this, and no configuration will change that.
     *
     * <p>⚠️ The message names all three things somebody needs — what was attempted, which identity
     * attempted it, and which kind of identity would have succeeded — because the fix is always to
     * use a different identity, and a refusal that does not say so leaves the reader guessing.
     */
    record CapabilityUnavailable(
            Capability   capability,
            String       identityName,
            IdentityKind kind,
            IdentityKind capableKind
    ) implements TelegramRefusal {

        @Override
        public String message() {
            if (capableKind == null) {
                return "%s is not available to identity '%s' (%s), and no installed transport provides it"
                        .formatted(capability, identityName, kind);
            }

            return "%s is not available to identity '%s' (%s); it requires a %s identity"
                    .formatted(capability, identityName, kind, capableKind);
        }

        @Override
        public boolean retryable() {
            return false;
        }
    }

    /** Telegram refused for a reason this library does not model separately. */
    record Rejected(int errorCode, String description) implements TelegramRefusal {

        @Override
        public String message() {
            return "Telegram refused the call (%d): %s".formatted(errorCode, description);
        }

        /**
         * ⚠️ Conservatively false. An unmodelled refusal retried in a loop is how a bot earns a flood
         * penalty for a call that was never going to work; a caller that knows better can retry
         * deliberately.
         */
        @Override
        public boolean retryable() {
            return false;
        }
    }

    /** Nothing answered — refused, timed out, DNS, a broken connection. */
    record TransportFailure(String detail) implements TelegramRefusal {

        @Override
        public String message() {
            return "Telegram could not be reached: %s".formatted(detail);
        }

        @Override
        public boolean retryable() {
            return true;
        }
    }
}
