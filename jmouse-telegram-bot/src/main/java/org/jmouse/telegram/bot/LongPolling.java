package org.jmouse.telegram.bot;

import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.update.Update;
import org.jmouse.telegram.update.UpdateDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Asks Telegram for updates in a loop, and feeds them to a dispatcher.
 *
 * <h2>⚠️ This is what development runs on here, not a fallback</h2>
 *
 * <p>A Telegram webhook requires a publicly reachable HTTPS address. This workspace's machine serves
 * plain HTTP and is behind a router, so a webhook cannot be registered against it at all. Polling is
 * therefore the arrangement that works during development, and the webhook is the production one —
 * which is why the choice is configuration rather than a decision taken once in code.
 *
 * <h2>⚠️ Exactly one of these may run per bot</h2>
 *
 * <p>Telegram delivers a bot's updates to one consumer. A second poller does not receive copies; it
 * takes updates the first will then never see, and the symptom is each instance handling roughly half
 * of everything at random. Two products sharing a bot share <em>this</em>, and fan out through the
 * {@link UpdateDispatcher}.
 *
 * <pre>{@code
 * try (LongPolling polling = new LongPolling(transport, identities, dispatcher)) {
 *     polling.start();
 *     // ... the application runs ...
 * }
 * }</pre>
 */
public final class LongPolling implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LongPolling.class);

    /** Long enough that an idle bot makes one request a minute; short enough to notice a dead socket. */
    public static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofSeconds(50);

    /** What to wait after an unreachable Telegram, so an outage is not a busy loop. */
    public static final Duration DEFAULT_FAILURE_BACKOFF = Duration.ofSeconds(5);

    private final AtomicBoolean running = new AtomicBoolean();

    private final UpdateFetcher    fetcher;
    private final IdentitySource   identities;
    private final UpdateDispatcher dispatcher;
    private final String           purpose;
    private final Duration         pollTimeout;
    private final Duration         failureBackoff;
    private final Set<String>      allowedUpdates;

    private volatile Thread worker;

    /** ⚠️ Not persisted. See {@link #offset()}. */
    private volatile long offset;

    public LongPolling(
            UpdateFetcher fetcher, IdentitySource identities, UpdateDispatcher dispatcher) {

        this(fetcher, identities, dispatcher, IdentitySource.GENERAL,
                DEFAULT_POLL_TIMEOUT, DEFAULT_FAILURE_BACKOFF, Set.of());
    }

    public LongPolling(
            UpdateFetcher    fetcher,
            IdentitySource   identities,
            UpdateDispatcher dispatcher,
            String           purpose,
            Duration         pollTimeout,
            Duration         failureBackoff,
            Set<String>      allowedUpdates) {

        this.fetcher        = Objects.requireNonNull(fetcher, "update fetcher");
        this.identities     = Objects.requireNonNull(identities, "identity source");
        this.dispatcher     = Objects.requireNonNull(dispatcher, "dispatcher");
        this.purpose        = purpose;
        this.pollTimeout    = pollTimeout;
        this.failureBackoff = failureBackoff;
        this.allowedUpdates = Set.copyOf(allowedUpdates);
    }

    /**
     * Starts the loop on its own named thread.
     *
     * <p>⚠️ Named, and not a daemon. A named thread is what makes "why is this application making a
     * request a minute" answerable from a thread dump; a non-daemon thread is what makes an unclosed
     * poller keep the JVM alive, which is a visible bug rather than a silent one.
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("this poller is already running");
        }

        worker = new Thread(this::loop, "telegram-long-polling");
        worker.start();
    }

    /**
     * Stops the loop and waits briefly for it to finish.
     *
     * <p>⚠️ Interrupts rather than only setting a flag. The thread spends almost all of its life
     * blocked inside a request Telegram is holding open for fifty seconds, so a flag alone would mean
     * a shutdown that takes up to a minute — and on a development machine, a restart that leaves the
     * previous poller stealing updates from the new one.
     */
    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        Thread current = worker;

        if (current != null) {
            current.interrupt();

            try {
                current.join(Duration.ofSeconds(5).toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * The next update id this poller will ask for.
     *
     * <p>⚠️ Held in memory only. Telegram keeps undelivered updates for 24 hours, so a restart resumes
     * from whatever it has not yet acknowledged rather than from zero — which means <em>redelivery</em>
     * of the last batch is normal after a crash, and a handler that must not act twice needs its own
     * idempotency. That is the outbox's job in `JMF-336`, not this loop's.
     */
    public long offset() {
        return offset;
    }

    private void loop() {
        LOGGER.info("telegram long polling started (purpose={}, timeout={}s)",
                purpose, pollTimeout.toSeconds());

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                pollOnce();
            } catch (CancellationException | InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (TelegramException exception) {
                if (!handleRefusal(exception)) {
                    break;
                }
            } catch (RuntimeException exception) {
                // Anything unforeseen: log it and carry on rather than ending ingestion silently.
                LOGGER.error("telegram polling hit an unexpected failure; continuing", exception);
                sleepQuietly(failureBackoff);
            }
        }

        running.set(false);
        LOGGER.info("telegram long polling stopped");
    }

    private void pollOnce() throws InterruptedException {
        TelegramIdentity identity = identities.identity(purpose);
        List<Update>     updates  = fetcher.fetchUpdates(identity, offset, pollTimeout, allowedUpdates);

        if (updates.isEmpty()) {
            return;
        }

        long highest = offset - 1;

        for (Update update : updates) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            dispatcher.dispatch(update);

            // ⚠️ Advanced only AFTER the update has been handed over, and only ever forwards. Raising
            // the offset before dispatching is how an update is lost instead of redelivered - Telegram
            // treats the next request carrying a higher offset as an acknowledgement.
            highest = Math.max(highest, update.updateId());
        }

        offset = highest + 1;
    }

    /** @return whether the loop should continue */
    private boolean handleRefusal(TelegramException exception) {
        TelegramRefusal refusal = exception.refusal();

        // ⚠️ A wrong or revoked token will never start working, and retrying it every five seconds
        // produces a log nobody can read past. Stopping makes the misconfiguration visible.
        if (refusal instanceof TelegramRefusal.Unauthorized) {
            LOGGER.error("telegram polling stopped: {}", refusal.message());
            return false;
        }

        if (refusal instanceof TelegramRefusal.FloodWait floodWait) {
            LOGGER.warn("telegram is rate-limiting polling; waiting {}ms",
                    floodWait.retryAfter().toMillis());
            sleepQuietly(floodWait.retryAfter());

            return true;
        }

        LOGGER.warn("telegram polling failed ({}); retrying in {}s",
                refusal.message(), failureBackoff.toSeconds());
        sleepQuietly(failureBackoff);

        return true;
    }

    private void sleepQuietly(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
