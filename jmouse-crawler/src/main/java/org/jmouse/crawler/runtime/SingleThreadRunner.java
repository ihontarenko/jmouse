package org.jmouse.crawler.runtime;

import org.jmouse.core.Verify;

import java.time.Instant;

public final class SingleThreadRunner implements CrawlRunner {

    private final int retryDrainBatch;

    public SingleThreadRunner() {
        this(256);
    }

    public SingleThreadRunner(int retryDrainBatch) {
        this.retryDrainBatch = Math.max(1, retryDrainBatch);
    }

    @Override
    public void runUntilDrained(CrawlEngine engine) {
        ParallelCrawlEngine parallelEngine = Verify.instanceOf(engine, ParallelCrawlEngine.class, "engine");

        while (true) {
            parallelEngine.moveReadyRetries(retryDrainBatch);

            CrawlTask task = parallelEngine.poll();
            if (task == null) {
                if (parallelEngine.frontierSize() == 0 && parallelEngine.retrySize() == 0) {
                    break;
                }
                continue;
            }

            Instant         now         = parallelEngine.now();
            TaskDisposition disposition = parallelEngine.execute(task, now);

            parallelEngine.apply(task, disposition, now);
        }
    }
}
