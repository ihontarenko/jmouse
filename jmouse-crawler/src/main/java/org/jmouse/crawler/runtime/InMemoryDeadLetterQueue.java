package org.jmouse.crawler.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class InMemoryDeadLetterQueue implements DeadLetterQueue {

    private final ConcurrentLinkedQueue<DeadLetterEntry> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void put(CrawlTask task, DeadLetterItem item) {
        if (task != null && item != null) {
            queue.offer(new DeadLetterEntry(task, item));
        }
    }

    @Override
    public List<DeadLetterEntry> pollBatch(int max) {
        if (max <= 0) {
            return List.of();
        }

        List<DeadLetterEntry> batch = new ArrayList<>(Math.min(max, 64));

        while (batch.size() < max) {
            DeadLetterEntry entry = queue.poll();

            if (entry == null) {
                break;
            }

            batch.add(entry);
        }

        return batch;
    }

    @Override
    public int size() {
        return queue.size();
    }
}
