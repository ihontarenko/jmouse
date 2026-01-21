package org.jmouse.crawler.runtime.state.persistence.snapshot;

import org.jmouse.crawler.api.ProcessingTask;

import java.util.List;

public record InFlightSnapshot(List<ProcessingTask> tasks) {
    public static InFlightSnapshot empty() {
        return new InFlightSnapshot(List.of());
    }
}
