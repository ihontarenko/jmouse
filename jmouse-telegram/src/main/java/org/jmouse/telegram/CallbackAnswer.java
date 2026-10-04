package org.jmouse.telegram;

import java.util.Objects;

/**
 * The reply owed to a button press.
 *
 * <h2>⚠️ Answering is mandatory, and forgetting it is a visible defect</h2>
 *
 * <p>When somebody presses an inline button, their client shows a progress indicator and keeps showing
 * it until the bot answers — for up to thirty seconds. A handler that does the work and never answers
 * produces a button that appears to hang, and then appears to fail, while the work it triggered
 * actually succeeded. {@link #acknowledge} exists so the cheapest correct answer is one word.
 *
 * @param queryId the callback query being answered
 * @param notice  a short message for the person, or {@code null} to answer silently
 * @param alert   {@code true} shows {@link #notice} as a dialog they must dismiss; {@code false} as a
 *                toast that fades. A dialog for routine confirmation is an interruption
 */
public record CallbackAnswer(String queryId, String notice, boolean alert) {

    /** Telegram's ceiling on the notice. */
    public static final int NOTICE_LIMIT = 200;

    public CallbackAnswer {
        Objects.requireNonNull(queryId, "callback query id");

        if (notice != null && notice.length() > NOTICE_LIMIT) {
            throw new IllegalArgumentException(
                    "a callback notice is at most %d characters; this one is %d"
                            .formatted(NOTICE_LIMIT, notice.length()));
        }
    }

    /** Stops the spinner and says nothing. The right answer when the message itself was edited. */
    public static CallbackAnswer acknowledge(String queryId) {
        return new CallbackAnswer(queryId, null, false);
    }

    /** A toast that fades by itself. */
    public static CallbackAnswer notice(String queryId, String notice) {
        return new CallbackAnswer(queryId, notice, false);
    }

    /** A dialog they have to dismiss — for a refusal or a warning, not for routine confirmation. */
    public static CallbackAnswer alert(String queryId, String notice) {
        return new CallbackAnswer(queryId, notice, true);
    }
}
