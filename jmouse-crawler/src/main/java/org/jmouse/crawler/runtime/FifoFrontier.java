package org.jmouse.crawler.runtime;

import org.jmouse.crawler.core.CrawlTask;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class FifoFrontier implements Frontier {

    private final ConcurrentLinkedQueue<CrawlTask> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void offer(CrawlTask task) {
        if (task != null) queue.offer(task);
    }

    @Override
    public CrawlTask poll() {
        return queue.poll();
    }

    @Override
    public int size() {
        return queue.size();
    }
}
