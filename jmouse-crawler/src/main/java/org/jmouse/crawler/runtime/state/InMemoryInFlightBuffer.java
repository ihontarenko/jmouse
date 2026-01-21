package org.jmouse.crawler.runtime.state;

import org.jmouse.crawler.api.InFlightBuffer;
import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.TaskId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryInFlightBuffer implements InFlightBuffer {

    private final Map<TaskId, ProcessingTask> map = new ConcurrentHashMap<>();

    @Override
    public void put(ProcessingTask task) {
        map.put(task.id(), task);
    }

    @Override
    public void remove(TaskId id) {
        map.remove(id);
    }

    @Override
    public List<ProcessingTask> drainAll() {
        List<ProcessingTask> all = new ArrayList<>(map.values());
        map.clear();
        return all;
    }

    @Override
    public int size() {
        return map.size();
    }

}
