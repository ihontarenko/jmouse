package org.jmouse.crawler.runtime.state.persistence.events;

import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.TaskId;

public sealed interface FrontierEvent permits FrontierEvent.Offer, FrontierEvent.Poll {

    static FrontierEvent offer(ProcessingTask task) {
        return new Offer(task);
    }

    static FrontierEvent poll(TaskId id) {
        return new Poll(id);
    }

    record Offer(ProcessingTask task) implements FrontierEvent {
    }

    record Poll(TaskId taskId) implements FrontierEvent {
    }
}
