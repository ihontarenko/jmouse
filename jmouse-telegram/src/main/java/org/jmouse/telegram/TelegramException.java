package org.jmouse.telegram;

import java.util.Objects;

/**
 * A refusal, thrown.
 *
 * <p>Unchecked, because almost nothing a caller does about a failed send belongs at the call site: a
 * notification that cannot be delivered is the outbox's problem, not the line that composed it. The
 * {@link #refusal()} is what carries the meaning — the exception is the delivery mechanism, and code
 * that switches on the refusal rather than on the exception type is code that keeps working when a
 * case is added.
 *
 * <pre>{@code
 * try {
 *     gateway.send(chat, draft);
 * } catch (TelegramException exception) {
 *     switch (exception.refusal()) {
 *         case TelegramRefusal.BotBlocked blocked -> bindings.deactivate(chat);
 *         case TelegramRefusal.FloodWait wait     -> outbox.deferBy(wait.retryAfter());
 *         default                                 -> outbox.failed(exception.refusal());
 *     }
 * }
 * }</pre>
 */
public class TelegramException extends RuntimeException {

    private final transient TelegramRefusal refusal;

    public TelegramException(TelegramRefusal refusal) {
        super(Objects.requireNonNull(refusal, "refusal").message());
        this.refusal = refusal;
    }

    public TelegramException(TelegramRefusal refusal, Throwable cause) {
        super(Objects.requireNonNull(refusal, "refusal").message(), cause);
        this.refusal = refusal;
    }

    public TelegramRefusal refusal() {
        return refusal;
    }

    /** Shorthand for the one question an outbox asks. */
    public boolean retryable() {
        return refusal.retryable();
    }
}
