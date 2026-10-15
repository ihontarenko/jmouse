package org.jmouse.telegram.journal;

import java.time.Duration;
import java.time.Instant;

/**
 * One call to Telegram, after the fact: what went where, when, as whom, and what Telegram said.
 *
 * <h2>⚠️ No message text, ever</h2>
 *
 * <p>The journal answers <em>"why did nothing arrive"</em>, which needs the destination, the identity,
 * the time and the refusal — not the words. Keeping the words would turn a diagnostic table into a
 * second copy of every private message the product ever sent, readable by whoever can read the
 * administration API. A caller who needs to know <em>which</em> message keeps the {@code messageId}
 * against its own record.
 *
 * @param at           when the call finished
 * @param identityName who spoke — the identity's name, ⚠️ never its credential
 * @param operation    what was asked
 * @param chat         the chat's wire value ({@code -100…} or {@code @name}), or {@code null} for a
 *                     callback answer, which names no chat
 * @param threadId     the forum topic, or {@code null}
 * @param messageId    the message sent, edited or deleted, or {@code null} when none came back
 * @param outcome      how it ended
 * @param refusal      the refusal's case name ({@code FloodWait}, {@code BotBlocked}…), or {@code null}
 * @param detail       the refusal's own sentence, or {@code null} when it went through
 * @param elapsed      how long the call took, retries and pacing waits included
 */
public record JournalEntry(
        Instant   at,
        String    identityName,
        Operation operation,
        String    chat,
        Integer   threadId,
        Integer   messageId,
        Outcome   outcome,
        String    refusal,
        String    detail,
        Duration  elapsed
) {

    /** What was asked of Telegram. */
    public enum Operation {
        SEND,
        EDIT,
        DELETE,
        ANSWER_CALLBACK
    }

    /** How a call ended. */
    public enum Outcome {

        /** Telegram accepted it. */
        DELIVERED,

        /** Telegram, or the gateway on its behalf, refused it — see {@code refusal} and {@code detail}. */
        REFUSED
    }

    public JournalEntry {
        if (at == null || operation == null || outcome == null) {
            throw new IllegalArgumentException("A journal entry needs a time, an operation and an outcome");
        }

        elapsed = elapsed == null ? Duration.ZERO : elapsed;
    }

    public boolean refused() {
        return outcome == Outcome.REFUSED;
    }
}
