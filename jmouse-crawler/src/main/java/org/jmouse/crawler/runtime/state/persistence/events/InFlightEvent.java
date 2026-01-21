package org.jmouse.crawler.runtime.state.persistence.events;

import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.TaskId;

public sealed interface InFlightEvent permits InFlightEvent.Put, InFlightEvent.Remove {

    static InFlightEvent put(ProcessingTask task) {
        return new Put(task);
    }

    static InFlightEvent remove(TaskId id) {
        return new Remove(id);
    }

    record Put(ProcessingTask task) implements InFlightEvent {
    }

    record Remove(TaskId taskId) implements InFlightEvent {
    }

}
