package org.jmouse.crawler.runtime;

import org.jmouse.core.Verify;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

public final class ExecutorRunner implements CrawlRunner {

    private final CrawlScheduler  scheduler;
    private final ExecutorService executor;
    private final int             maxInFlight;

    public ExecutorRunner(CrawlScheduler scheduler, ExecutorService executor, int maxInFlight) {
        this.scheduler = Verify.nonNull(scheduler, "scheduler");
        this.executor = Verify.nonNull(executor, "executor");
        this.maxInFlight = Math.max(1, maxInFlight);
    }

    @Override
    public void runUntilDrained(CrawlEngine engine) {
        ParallelCrawlEngine     parallelEngine    = Verify.instanceOf(engine, ParallelCrawlEngine.class, "engine");
        CompletionService<Done> completionService = new ExecutorCompletionService<>(executor);
        int                     inFlight          = 0;

        while (true) {
            while (inFlight < maxInFlight) {
                ScheduleDecision decision = scheduler.nextDecision();

                if (decision instanceof ScheduleDecision.TaskReady(CrawlTask task, Instant now)) {
                    completionService.submit(() -> {
                        TaskDisposition disposition = parallelEngine.execute(task, now);
                        return new Done(task, disposition, now);
                    });
                    inFlight++;
                    continue;
                }

                if (decision instanceof ScheduleDecision.Park ignore) {
                    drainCompletions(completionService, parallelEngine, Duration.ZERO, inFlight);
                    break;
                }

                if (decision instanceof ScheduleDecision.Drained) {
                    break;
                }
            }

            // drain at least one completion or park briefly
            if (inFlight > 0) {
                Done done = pollWithTimeout(completionService, 50);
                if (done != null) {
                    parallelEngine.apply(done.task(), done.disposition(), done.now());
                    inFlight--;
                }
            }

            // final stop condition: scheduler drained AND no running tasks
            if (inFlight == 0) {
                ScheduleDecision decision = scheduler.nextDecision();
                if (decision instanceof ScheduleDecision.Drained) {
                    return;
                }
                if (decision instanceof ScheduleDecision.Park park) {
                    park(park.duration());
                }
                if (decision instanceof ScheduleDecision.TaskReady taskReady) {
                    // put back into scheduler flow: easiest is to submit it back
                    scheduler.submit(taskReady.task());
                }
            }
        }
    }

    private static Done pollWithTimeout(CompletionService<Done> completionService, long millis) {
        try {
            Future<Done> future = completionService.poll(millis, TimeUnit.MILLISECONDS);
            return future != null ? getQuietly(future) : null;
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

    private static void drainCompletions(
            CompletionService<Done> completionService,
            ParallelCrawlEngine engine,
            Duration timeout,
            int inFlight
    ) {
        // optional: keep minimal (can be expanded)
    }

    private static void park(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            Thread.onSpinWait();
            return;
        }
        try {
            Thread.sleep(Math.min(duration.toMillis(), 50));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record Done(CrawlTask task, TaskDisposition disposition, Instant now) { }
}
