package org.jmouse.scheduler;

import org.jmouse.scheduler.spi.TriggerDispatcher;
import org.jmouse.scheduler.spi.TriggerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Runs each occurrence's handler in this process, on a virtual thread per occurrence, so the polling
 * thread is never held by the work.
 *
 * <p>The dispatcher for a product without a job queue of its own. It is not durable: work in flight
 * when the process stops is lost, and a handler failure is only logged — the occurrence was already
 * recorded as {@link OccurrenceOutcome#FIRED} when it was handed over. ⚠️ Nor is it idempotent: after a
 * crash between dispatch and confirmation, the handler runs again for the same occurrence, so handlers
 * behind it must tolerate that. A product that needs any of these properties dispatches into its own
 * durable queue, keyed by {@link Occurrence#identity()}, instead.
 */
public final class InProcessDispatcher implements TriggerDispatcher, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(InProcessDispatcher.class);

    private final HandlerRegistry handlers;
    private final ExecutorService executor;

    /**
     * @param handlers the handlers occurrences resolve against
     */
    public InProcessDispatcher(HandlerRegistry handlers) {
        this.handlers = Objects.requireNonNull(handlers, "handlers");
        this.executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("jmouse-scheduler-handler-", 0).factory());
    }

    @Override
    public void dispatch(Occurrence occurrence) {
        TriggerHandler handler = handlers.require(occurrence.handlerKey());
        executor.execute(() -> runHandler(handler, occurrence));
    }

    /** Stops accepting work and waits up to thirty seconds for running handlers. */
    @Override
    public void close() {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static void runHandler(TriggerHandler handler, Occurrence occurrence) {
        try {
            handler.handle(occurrence);
        } catch (RuntimeException exception) {
            LOGGER.error("jmouse-scheduler: handler '{}' failed for {}", handler.key(), occurrence.identity(), exception);
        }
    }
}
