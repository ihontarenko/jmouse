package org.jmouse.notifier.spring;

import org.jmouse.notifier.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Calls {@link Notifier#retryDue()} on an interval — the thread the notifier deliberately does not own.
 *
 * <p>A thread of its own rather than {@code @Scheduled}: a library must not require the product to have
 * enabled scheduling, and nothing would say so if it had not — retries would simply never happen.
 *
 * <p>⚠️ {@link AutoCloseable}, so Spring stops it on shutdown.
 */
public final class NotifierRetryWorker implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifierRetryWorker.class);

    private final Notifier                 notifier;
    private final Duration                 interval;
    private final ScheduledExecutorService executor;

    public NotifierRetryWorker(Notifier notifier, Duration interval) {
        this.notifier = notifier;
        this.interval = interval;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jmouse-notifier-retry");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        executor.scheduleWithFixedDelay(this::retry, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void retry() {
        try {
            int attempted = notifier.retryDue();

            if (attempted > 0) {
                LOGGER.debug("retried {} notification deliveries", attempted);
            }
        } catch (RuntimeException exception) {
            // ⚠️ Caught: an exception escaping a scheduled task cancels every later run, silently.
            LOGGER.warn("notification retries failed; will try again in {}", interval, exception);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
