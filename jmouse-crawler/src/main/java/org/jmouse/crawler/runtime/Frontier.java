package org.jmouse.crawler.runtime;

public interface Frontier {
    void offer(ProcessingTask task);
    ProcessingTask poll();
    int size();
}
