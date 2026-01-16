package org.jmouse.crawler.runtime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

public final class InMemoryRetryBuffer implements RetryBuffer {

    private final PriorityBlockingQueue<Item> queue = new PriorityBlockingQueue<>(128, Comparator.comparing(Item::notBefore));

    @Override
    public void schedule(CrawlTask task, Instant notBefore, String reason, Throwable error) {
        if (task != null && notBefore != null) {
            queue.offer(new Item(notBefore, task, reason, error));
        }
    }

    @Override
    public List<CrawlTask> drainReady(Instant now, int max) {
        if (now == null || max <= 0) {
            return List.of();
        }

        List<CrawlTask> ready = new ArrayList<>(Math.min(max, 64));

        while (ready.size() < max) {
            Item head = queue.peek();

            if (head == null || head.notBefore().isAfter(now)) {
                break;
            }

            queue.poll();
            ready.add(head.task());
        }

        return ready;
    }

    @Override
    public int size() {
        return queue.size();
    }

    private record Item(Instant notBefore, CrawlTask task, String reason, Throwable error) { }
}
