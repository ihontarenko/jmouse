package org.jmouse.crawler.api;

import java.util.List;

public interface InFlightBuffer {

    void put(ProcessingTask task);

    void remove(TaskId id);

    List<ProcessingTask> drainAll();

    int size();

}
