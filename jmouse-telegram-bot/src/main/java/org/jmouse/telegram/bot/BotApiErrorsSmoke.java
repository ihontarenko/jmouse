package org.jmouse.telegram.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmouse.telegram.TelegramRefusal;

import java.time.Duration;

import static org.jmouse.telegram.smoke.Checks.check;
import static org.jmouse.telegram.smoke.Checks.report;

/**
 * Checks how Telegram's error answers become refusals. Run its {@code main}.
 *
 * <p>⚠️ In this package rather than under {@code smoke/}, because {@link BotApiErrors#translate} is
 * package-private on purpose — the matching of English sentences stays inside one file — and a smoke
 * class is not a reason to widen it.
 */
public final class BotApiErrorsSmoke {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] arguments) throws Exception {
        check("429 obeys retry_after", BotApiErrors.translate(
                429, "Too Many Requests: retry after 17", JSON.readTree("{\"retry_after\":17}"), "1")
                instanceof TelegramRefusal.FloodWait wait && wait.retryAfter().equals(Duration.ofSeconds(17)));
        check("a blocked bot is BotBlocked", BotApiErrors.translate(
                403, "Forbidden: bot was blocked by the user", null, "1") instanceof TelegramRefusal.BotBlocked);
        check("a bot nobody started is ConversationNotStarted, not BotBlocked", BotApiErrors.translate(
                403, "Forbidden: bot can't initiate conversation with a user", null, "1")
                instanceof TelegramRefusal.ConversationNotStarted);
        check("a missing chat is ChatNotFound", BotApiErrors.translate(
                400, "Bad Request: chat not found", null, "1") instanceof TelegramRefusal.ChatNotFound);
        check("an unknown answer is Rejected with its code", BotApiErrors.translate(
                400, "Bad Request: something new", null, "1") instanceof TelegramRefusal.Rejected rejected
                && rejected.errorCode() == 400);
        report();
    }
}
