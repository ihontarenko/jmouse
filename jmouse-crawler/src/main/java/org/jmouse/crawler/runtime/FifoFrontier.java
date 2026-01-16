package org.jmouse.crawler.runtime;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class FifoFrontier implements Frontier {

    private final ConcurrentLinkedQueue<ProcessingTask> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void offer(ProcessingTask task) {
        if (task != null) queue.offer(task);
    }

    @Override
    public ProcessingTask poll() {
        return queue.poll();
    }

    @Override
    public int size() {
        return queue.size();
    }
}
