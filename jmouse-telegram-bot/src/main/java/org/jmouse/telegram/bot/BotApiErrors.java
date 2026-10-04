package org.jmouse.telegram.bot;

import com.fasterxml.jackson.databind.JsonNode;
import org.jmouse.telegram.TelegramRefusal;

import java.time.Duration;
import java.util.Locale;

/**
 * Turns Telegram's prose into a {@link TelegramRefusal}.
 *
 * <h2>⚠️ The one place in this library that reads an error description</h2>
 *
 * <p>A refused Bot API call answers with {@code {"ok":false,"error_code":403,"description":"Forbidden:
 * bot was blocked by the user"}}. The code is coarse — 400 covers a missing chat, an unchanged edit, a
 * malformed keyboard and a caption that is too long — so the description is the only thing that
 * distinguishes them, and it is an English sentence written for a person.
 *
 * <p>That makes matching on it unavoidable and also fragile: the wording is not a contract and has
 * been changed before. The containment is the point. It happens <strong>here, once</strong>, so the
 * day a phrase changes there is one file to fix — rather than a {@code description.contains("blocked")}
 * in every product, each subtly different and each silently wrong.
 *
 * <p>Anything unrecognised becomes {@link TelegramRefusal.Rejected} carrying the original code and
 * text. ⚠️ It is deliberately <em>not</em> retryable: an unmodelled refusal retried in a loop is how a
 * bot earns a flood penalty for a call that was never going to work.
 */
final class BotApiErrors {

    private BotApiErrors() {
    }

    static TelegramRefusal translate(int errorCode, String description, JsonNode parameters, String chat) {
        String text = description == null ? "" : description.toLowerCase(Locale.ROOT);

        // Telegram states the delay itself; obeying it beats any backoff we could invent.
        if (errorCode == 429) {
            return new TelegramRefusal.FloodWait(retryAfter(parameters));
        }

        if (errorCode == 401) {
            return new TelegramRefusal.Unauthorized("bot", description);
        }

        // ⚠️ The one refusal with a bookkeeping consequence rather than a retry: the person turned the
        // bot off, and the binding that says they are reachable has stopped being true.
        if (text.contains("bot was blocked by the user")
                || text.contains("user is deactivated")
                || text.contains("bot was kicked")) {
            return new TelegramRefusal.BotBlocked(chat);
        }

        if (text.contains("chat not found")
                || text.contains("peer_id_invalid")
                || text.contains("user not found")) {
            return new TelegramRefusal.ChatNotFound(chat);
        }

        // Benign: a periodic updater recomputing identical text hits this on every run.
        if (text.contains("message is not modified")) {
            return new TelegramRefusal.MessageNotModified();
        }

        if (text.contains("not enough rights")
                || text.contains("have no rights")
                || text.contains("need administrator rights")
                || text.contains("can't remain") ) {
            return new TelegramRefusal.InsufficientRights(chat, description);
        }

        // ⚠️ Telegram reports an invalid token as 404 on the method path rather than as 401, which
        // reads as "this method does not exist" and sends people looking in the wrong place.
        if (errorCode == 404) {
            return new TelegramRefusal.Unauthorized("bot",
                    "the method or the token is wrong (Telegram answers 404 for an invalid token)");
        }

        return new TelegramRefusal.Rejected(errorCode, description);
    }

    /**
     * ⚠️ Telegram puts the delay in {@code parameters.retry_after}, not in a header. A missing figure
     * falls back to a second rather than to zero — a retry with no delay at all is what turns one
     * rate-limit into a sustained penalty.
     */
    private static Duration retryAfter(JsonNode parameters) {
        if (parameters != null && parameters.hasNonNull("retry_after")) {
            return Duration.ofSeconds(parameters.get("retry_after").asLong());
        }

        return Duration.ofSeconds(1);
    }
}
