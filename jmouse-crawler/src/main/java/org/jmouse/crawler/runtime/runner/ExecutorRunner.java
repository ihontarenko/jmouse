package org.jmouse.crawler.runtime.runner;

import org.jmouse.core.Verify;
import org.jmouse.core.context.execution.ExecutionContextHolder;
import org.jmouse.crawler.api.ProcessingEngine;
import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.runtime.schedule.ScheduleDecision;
import org.jmouse.crawler.runtime.core.TaskDisposition;
import org.jmouse.crawler.runtime.schedule.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

/**
 * Scheduler runner that executes {@link ProcessingTask}s using an {@link ExecutorService}
 * and applies results back into the {@link ProcessingEngine} as completions arrive. ⚙️
 *
 * <p>Key goals:</p>
 * <ul>
 *   <li>Maintain up to {@code maxInFlight} concurrent tasks</li>
 *   <li>Apply completed results promptly (prefer non-blocking drains)</li>
 *   <li>Respect scheduler decisions such as {@code Park} without busy-spinning</li>
 * </ul>
 */
public final class ExecutorRunner extends AbstractSchedulerRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorRunner.class);

    private final ExecutorService executor;
    private final int             maxInFlight;

    /**
     * @param scheduler    job scheduler
     * @param clock        time source used for apply timestamps
     * @param executor     executor for parallel task execution
     * @param maxInFlight  maximum number of concurrently running tasks (min 1)
     */
    public ExecutorRunner(JobScheduler scheduler, Clock clock, ExecutorService executor, int maxInFlight) {
        super(scheduler, clock);
        this.executor = Verify.nonNull(executor, "executor");
        this.maxInFlight = Math.max(1, maxInFlight);
    }

    @Override
    public void runUntilDrained(ProcessingEngine engine) {
        ProcessingEngine processingEngine = requireEngine(engine);

        CompletionService<Done> completion = new ExecutorCompletionService<>(executor);

        int              inFlight = 0;
        ScheduleDecision stashed  = null;

        LOGGER.info(
                "Started (maxInFlight={}, executor={})",
                maxInFlight,
                executor
        );

        while (true) {

            // 1) Fill capacity
            while (inFlight < maxInFlight) {
                ScheduleDecision decision = nextDecisionOrStashed(stashed);
                stashed = null;

                if (decision instanceof ScheduleDecision.TaskReady(ProcessingTask task)) {
                    submit(completion, processingEngine, task);
                    inFlight++;
                    continue;
                }

                // Can't submit more tasks right now.
                stashed = decision;
                break;
            }

            // 2) Apply any completed work (non-blocking drain)
            inFlight -= drainAvailable(completion, processingEngine, clock, inFlight);

            // 3) Decide what to do next
            ScheduleDecision decision = nextDecisionOrStashed(stashed);
            stashed = null;

            if (decision instanceof ScheduleDecision.Drained) {
                if (inFlight == 0) {
                    return;
                }

                // Scheduler drained, but workers are still running -> wait for at least one completion.
                if (awaitAndApplyOne(completion, processingEngine, clock, Await.take())) {
                    inFlight--;
                }
                continue;
            }

            if (decision instanceof ScheduleDecision.Park park) {
                if (inFlight == 0) {
                    // Nothing running -> simply park for the suggested duration.
                    park(park.duration());
                    continue;
                }

                // Some work is running. Try to pick up a completion within a small bounded wait.
                // If none arrives, park to avoid CPU spin while still respecting scheduler backoff.
                if (awaitAndApplyOne(completion, processingEngine, clock, Await.poll(park.duration(), 50))) {
                    inFlight--;
                } else {
                    park(park.duration());
                }
                continue;
            }

            if (decision instanceof ScheduleDecision.TaskReady taskReady) {
                // We got a ready task, but capacity was full in the fill loop.
                // Stash it and loop; the fill loop will submit it next iteration.
                stashed = taskReady;
            }
        }
    }

    /**
     * Submit a task for execution; the worker returns a {@link Done} object
     * that pairs the task with its disposition. 🧵
     */
    private static void submit(
            CompletionService<Done> service,
            ProcessingEngine processingEngine,
            ProcessingTask task
    ) {
        LOGGER.trace(
                "Submit task url={}, attempt={}, scheduledAt={}",
                task.url(),
                task.attempt(),
                task.scheduledAt()
        );
        service.submit(() -> new Done(task, processingEngine.execute(task)));
        service.submit(() -> {
            try (var ignored = ExecutionContextHolder.open(
                    ExecutionContextHolder.current().with(TraceKeys.TRACE, task.trace())
            )) {
                TaskDisposition disposition   = processingEngine.execute(task);
                return new Done(task, disposition);
            }
        });
    }

    /**
     * Drain all currently available completions (non-blocking) and apply them.
     *
     * @param limit maximum number of completions to apply (typically current in-flight)
     * @return number of applied completions
     */
    private static int drainAvailable(
            CompletionService<Done> completionService,
            ProcessingEngine engine,
            Clock clock,
            int limit
    ) {
        int applied = 0;

        while (applied < limit) {
            Done done = awaitDone(completionService, Await.poll());

            if (done == null) {
                break;
            }

            apply(engine, clock, done);

            applied++;
        }

        return applied;
    }

    /**
     * Await one completion according to {@link Await} and apply it if available.
     *
     * @return {@code true} if one completion was applied, otherwise {@code false}
     */
    private static boolean awaitAndApplyOne(
            CompletionService<Done> completionService,
            ProcessingEngine engine,
            Clock clock,
            Await await
    ) {
        Done done = awaitDone(completionService, await);

        if (done == null) {
            return false;
        }

        apply(engine, clock, done);

        return true;
    }

    /**
     * Wait for a completion using the provided strategy and unwrap it safely.
     *
     * <p>Interruption is treated as "no completion available right now" and
     * the interrupt flag is restored. ⛔</p>
     */
    private static Done awaitDone(CompletionService<Done> completionService, Await await) {
        try {
            return quietly(await.await(completionService));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Apply a completed task result to the engine with a timestamp. ⏱️
     */
    private static void apply(ProcessingEngine engine, Clock clock, Done done) {
        LOGGER.trace(
                "Task completed url={}, disposition={}",
                done.task().url(),
                done.disposition()
        );
        engine.apply(done.task(), done.disposition(), clock.instant());
    }

    /**
     * Extract the {@link Done} value from a {@link Future}.
     * Converts execution failures into unchecked exceptions.
     */
    private static Done quietly(Future<Done> future) {
        if (future == null) {
            return null;
        }

        try {
            return future.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static ScheduleDecision nextDecisionOrStashed(ScheduleDecision stashed) {
        return (stashed != null) ? stashed : schedulerNextDecision();
    }

    /**
     * Isolated to make the call site read cleanly (and to keep all scheduling in one place).
     */
    private static ScheduleDecision schedulerNextDecision() {
        // This method relies on the inherited 'scheduler' from AbstractSchedulerRunner.
        // Keeping it behind a method helps readability and future instrumentation.
        return ExecutorRunnerHolder.SCHEDULER.nextDecision();
    }

    /**
     * Completion wait strategy. 🎛️
     */
    private interface Await {

        Future<Done> await(CompletionService<Done> completionService) throws InterruptedException;

        static Await poll() {
            return CompletionService::poll;
        }

        static Await take() {
            return CompletionService::take;
        }

        static Await poll(Duration duration, long maxMillis) {
            long millis = clampMillis(duration, maxMillis);
            return (millis <= 0) ? poll() : service -> service.poll(millis, TimeUnit.MILLISECONDS);
        }

        private static long clampMillis(Duration duration, long maxMillis) {
            if (duration == null) {
                return 0L;
            }

            long milliSeconds = duration.toMillis();

            if (milliSeconds < 0L) {
                milliSeconds = 0L;
            }

            return Math.min(milliSeconds, Math.max(0L, maxMillis));
        }
    }

    /**
     * Completion marker for one executed {@link ProcessingTask}.
     */
    private record Done(ProcessingTask task, TaskDisposition disposition) {}

    /**
     * Small holder to keep the static scheduler accessor explicit and avoid accidental
     * capture of instance state in static helpers.
     *
     * <p>If you prefer, we can remove this and make {@code nextDecisionOrStashed}
     * an instance method instead (cleaner, fewer indirections).</p>
     */
    private static final class ExecutorRunnerHolder {

        private static JobScheduler SCHEDULER;

        private ExecutorRunnerHolder() {}

        static void bind(JobScheduler scheduler) {
            SCHEDULER = scheduler;
        }

    }
}
