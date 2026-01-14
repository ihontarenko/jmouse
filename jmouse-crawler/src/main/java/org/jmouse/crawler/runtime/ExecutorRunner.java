package org.jmouse.crawler.runtime;

import org.jmouse.crawler.core.CrawlTask;
import org.jmouse.crawler.core.TaskDisposition;

import java.time.Instant;
import java.util.concurrent.*;

public final class ExecutorRunner implements CrawlRunner {

    private final ExecutorService executor;
    private final int maxInFlight;
    private final int retryDrainBatch;

    public ExecutorRunner(ExecutorService executor, int maxInFlight) {
        this(executor, maxInFlight, 256);
    }

    public ExecutorRunner(ExecutorService executor, int maxInFlight, int retryDrainBatch) {
        this.executor = executor;
        this.maxInFlight = Math.max(1, maxInFlight);
        this.retryDrainBatch = Math.max(1, retryDrainBatch);
    }

    @Override
    public void runUntilDrained(CrawlEngine engine) {
        if (!(engine instanceof ParallelCrawlEngine pe)) {
            throw new IllegalArgumentException("Engine does not support parallel execution: " + engine.getClass().getName());
        }

        CompletionService<Done> cs = new ExecutorCompletionService<>(executor);
        int inFlight = 0;

        while (true) {

            // 1) move ready retries
            pe.moveReadyRetries(retryDrainBatch);

            // 2) submit tasks while we have capacity
            while (inFlight < maxInFlight) {
                CrawlTask task = pe.poll();
                if (task == null) break;

                Instant now = pe.now();
                cs.submit(() -> new Done(task, pe.execute(task, now), now));
                inFlight++;
            }

            // 3) drain completions (apply in runner thread)
            int applied = 0;
            while (inFlight > 0) {
                Future<Done> f = cs.poll();
                if (f == null) break;

                Done done = getQuietly(f);
                pe.apply(done.task(), done.disposition(), done.now());
                inFlight--;
                applied++;
            }

            // 4) stop condition: nothing queued, nothing delayed, nothing running
            if (pe.frontierSize() == 0 && pe.retrySize() == 0 && inFlight == 0) {
                break;
            }

            // 5) avoid busy spin:
            // if nothing to submit and nothing applied, but tasks are still running -> wait briefly for one completion
            if (applied == 0 && pe.frontierSize() == 0 && inFlight > 0) {
                Done done = takeWithTimeout(cs, 50);
                if (done != null) {
                    pe.apply(done.task(), done.disposition(), done.now());
                    inFlight--;
                }
            }
        }
    }

    private static Done getQuietly(Future<Done> f) {
        try {
            return f.get();
        } catch (ExecutionException e) {
            // worker threw unexpectedly; treat as DLQ at runner-level would require engine access.
            // Here we rethrow to make it visible; alternatively map to deadLetter.
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static Done takeWithTimeout(CompletionService<Done> cs, long millis) {
        try {
            Future<Done> f = cs.poll(millis, TimeUnit.MILLISECONDS);
            return (f != null) ? getQuietly(f) : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private record Done(CrawlTask task, TaskDisposition disposition, Instant now) {}
}
