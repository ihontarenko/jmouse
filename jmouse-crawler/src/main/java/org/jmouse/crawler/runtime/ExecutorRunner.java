package org.jmouse.crawler.runtime;

import org.jmouse.core.Verify;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.*;

public final class ExecutorRunner extends AbstractSchedulerRunner {

    private final ExecutorService executor;
    private final int             maxInFlight;

    public ExecutorRunner(JobScheduler scheduler, Clock clock, ExecutorService executor, int maxInFlight) {
        super(scheduler, clock);
        this.executor = Verify.nonNull(executor, "executor");
        this.maxInFlight = Math.max(1, maxInFlight);
    }

    @Override
    public void runUntilDrained(ProcessingEngine engine) {
        ProcessingEngine        parallelEngine    = requireParallel(engine);
        CompletionService<Done> completionService = new ExecutorCompletionService<>(executor);
        int                     inFlight          = 0;
        ScheduleDecision        lastDecision      = null;

        while (true) {

            // 1) Fill capacity
            while (inFlight < maxInFlight) {
                ScheduleDecision decision = (lastDecision != null) ? lastDecision : scheduler.nextDecision();
                lastDecision = null;

                if (decision instanceof ScheduleDecision.TaskReady(ProcessingTask task)) {
                    // no now var
                    completionService.submit(() -> new Done(task, parallelEngine.execute(task)));
                    inFlight++;
                    continue;
                }

                // Can't submit more tasks right now.
                lastDecision = decision;
                break;
            }

            // 2) Apply any completed work (non-blocking first)
            int applied = drainCompletedNonBlocking(completionService, parallelEngine, clock, inFlight);
            inFlight -= applied;

            // 3) Decide what to do next
            ScheduleDecision decision = (lastDecision != null) ? lastDecision : scheduler.nextDecision();
            lastDecision = null;

            if (decision instanceof ScheduleDecision.Drained) {
                if (inFlight == 0) {
                    return;
                }

                // Scheduler drained, but workers are still running -> wait for at least one completion.
                Done done = takeOne(completionService);
                if (done != null) {
                    parallelEngine.apply(done.task(), done.disposition(), clock.instant());
                    inFlight--;
                }
                continue;
            }

            if (decision instanceof ScheduleDecision.Park park) {
                if (inFlight == 0) {
                    // nothing running -> just park
                    park(park.duration());
                    continue;
                }

                // Some work is running. We can either:
                // - wait up to park.duration() for one completion, or
                // - if duration is very small, just do a short poll.
                Done done = pollOne(completionService, park.duration());
                if (done != null) {
                    parallelEngine.apply(done.task(), done.disposition(), clock.instant());
                    inFlight--;
                } else {
                    // no completion -> park briefly (avoids busy spin)
                    park(park.duration());
                }
                continue;
            }

            if (decision instanceof ScheduleDecision.TaskReady taskReady) {
                // We reached here because inFlight==maxInFlight in fill loop.
                // Stash decision and proceed to next iteration; fill loop will submit it.
                lastDecision = taskReady;
            }
        }
    }

    private static int drainCompletedNonBlocking(
            CompletionService<Done> completionService,
            ProcessingEngine engine,
            Clock clock, int inFlight
    ) {
        int applied = 0;

        while (inFlight - applied > 0) {
            Future<Done> future = completionService.poll();
            if (future == null) {
                break;
            }

            Done done = getQuietly(future);
            engine.apply(done.task(), done.disposition(), clock.instant());
            applied++;
        }

        return applied;
    }

    private static Done pollOne(CompletionService<Done> completionService, Duration duration) {
        long millis = (duration == null) ? 0L : Math.max(0L, Math.min(duration.toMillis(), 50L));

        try {
            Future<Done> future = (millis == 0L)
                    ? completionService.poll()
                    : completionService.poll(millis, TimeUnit.MILLISECONDS);
            return (future != null) ? getQuietly(future) : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static Done takeOne(CompletionService<Done> completionService) {
        try {
            Future<Done> future = completionService.take();
            return getQuietly(future);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static Done getQuietly(Future<Done> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private record Done(ProcessingTask task, TaskDisposition disposition) {}
}
