package org.jmouse.telegram.spring;

import org.jmouse.telegram.journal.TelegramJournal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Forgets journal entries older than the retention, once an hour.
 *
 * <p>A thread of its own rather than {@code @Scheduled}: a library must not require the product to have
 * enabled scheduling — and if it had not, nothing would say so; the table would simply grow.
 *
 * <p>⚠️ {@link AutoCloseable}, so Spring stops the thread on shutdown — a development restart must not
 * leave one behind.
 */
public final class TelegramJournalRetention implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramJournalRetention.class);

    /** How often to prune. Nobody needs an entry to vanish to the minute. */
    public static final Duration INTERVAL = Duration.ofHours(1);

    private final TelegramJournal          journal;
    private final Duration                 retention;
    private final ScheduledExecutorService executor;

    public TelegramJournalRetention(TelegramJournal journal, Duration retention) {
        this.journal   = Objects.requireNonNull(journal, "journal");
        this.retention = Objects.requireNonNull(retention, "retention");
        this.executor  = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jmouse-telegram-journal-retention");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        executor.scheduleWithFixedDelay(this::prune, 1, INTERVAL.toMinutes(), TimeUnit.MINUTES);
    }

    /** One pass; public so an administrator's action can run it on demand. */
    public long prune() {
        try {
            long forgotten = journal.prune(Instant.now().minus(retention));

            if (forgotten > 0) {
                LOGGER.info("pruned {} Telegram journal entries older than {}", forgotten, retention);
            }

            return forgotten;
        } catch (RuntimeException exception) {
            // ⚠️ Caught: an exception escaping a scheduled task cancels every later run, silently.
            LOGGER.warn("the Telegram journal could not be pruned; will try again in {}", INTERVAL, exception);
            return 0;
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
