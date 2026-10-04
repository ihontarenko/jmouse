package org.jmouse.telegram.pace;

import org.jmouse.telegram.CallbackAnswer;
import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.SentMessage;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.spi.TelegramTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

/**
 * Any transport, paced and retried.
 *
 * <p>A decorator rather than behaviour inside {@code BotApiTransport}, for three reasons: the MTProto
 * transport will need exactly the same treatment and would otherwise copy it; an application that has
 * an API gateway doing this already can leave the
 * decorator off; and a transport with no waiting or retrying in it is one whose failure paths can
 * actually be reasoned about.
 *
 * <h2>⚠️ What is retried, and what is not</h2>
 *
 * <p>Only {@link TelegramRefusal#retryable()}. The distinction is the entire point of the taxonomy:
 * retrying a {@link TelegramRefusal.ChatNotFound} forever fills a queue with work that can never
 * succeed, and <em>not</em> retrying a {@link TelegramRefusal.TransportFailure} loses a message to a
 * thirty-second outage.
 *
 * <p>⚠️ A {@link TelegramRefusal.FloodWait} sleeps <strong>Telegram's own figure</strong> and tells
 * the {@link Pace} about it. An invented exponential backoff is worse in both directions: shorter
 * gets refused again and extends the penalty, longer wastes the delivery window.
 */
public final class PacedTransport implements TelegramTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(PacedTransport.class);

    /** Enough to ride out a flood wait and a brief outage; few enough to fail visibly. */
    public static final int DEFAULT_MAX_ATTEMPTS = 3;

    /** What to wait after a transport failure, which — unlike a flood wait — names no figure. */
    public static final Duration DEFAULT_TRANSPORT_BACKOFF = Duration.ofSeconds(2);

    private final TelegramTransport delegate;
    private final Pace              pace;
    private final int               maxAttempts;
    private final Duration          transportBackoff;

    public PacedTransport(TelegramTransport delegate, Pace pace) {
        this(delegate, pace, DEFAULT_MAX_ATTEMPTS, DEFAULT_TRANSPORT_BACKOFF);
    }

    public PacedTransport(
            TelegramTransport delegate, Pace pace, int maxAttempts, Duration transportBackoff) {

        this.delegate         = Objects.requireNonNull(delegate, "delegate transport");
        this.pace             = Objects.requireNonNull(pace, "pace");
        this.maxAttempts      = maxAttempts;
        this.transportBackoff = transportBackoff;
    }

    @Override
    public IdentityKind kind() {
        return delegate.kind();
    }

    @Override
    public boolean supports(Capability capability) {
        return delegate.supports(capability);
    }

    @Override
    public SentMessage send(TelegramIdentity identity, ChatReference chat, MessageDraft draft) {
        return attempt(chat, () -> delegate.send(identity, chat, draft));
    }

    @Override
    public SentMessage edit(TelegramIdentity identity, MessageHandle message, MessageDraft draft) {
        return attempt(message.chat(), () -> delegate.edit(identity, message, draft));
    }

    @Override
    public void delete(TelegramIdentity identity, MessageHandle message) {
        attempt(message.chat(), () -> {
            delegate.delete(identity, message);
            return null;
        });
    }

    @Override
    public void answerCallback(TelegramIdentity identity, CallbackAnswer answer) {
        // ⚠️ Not paced and not retried. A callback answer is owed within seconds — the person's client
        // is showing a spinner — so waiting for a permit would produce the very hang the answer exists
        // to prevent. It is also a trivially cheap call that Telegram does not count against the
        // message rate.
        delegate.answerCallback(identity, answer);
    }

    private <T> T attempt(ChatReference chat, Supplier<T> call) {
        TelegramException last = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            pace.awaitTurn(chat);

            try {
                return call.get();
            } catch (TelegramException exception) {
                last = exception;

                if (!exception.retryable()) {
                    throw exception;
                }

                if (attempt == maxAttempts) {
                    break;
                }

                waitBefore(chat, exception.refusal(), attempt);
            }
        }

        throw last;
    }

    private void waitBefore(ChatReference chat, TelegramRefusal refusal, int attempt) {
        Duration delay = transportBackoff;

        if (refusal instanceof TelegramRefusal.FloodWait floodWait) {
            delay = floodWait.retryAfter();
            pace.penalise(chat, delay);
        }

        LOGGER.warn("telegram refused ({}), retrying in {}ms — attempt {} of {}",
                refusal.message(), delay.toMillis(), attempt, maxAttempts);

        sleep(delay);
    }

    private void sleep(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CancellationException("interrupted while waiting to retry a Telegram call");
        }
    }
}
