package org.jmouse.crawler.runtime.state.persistence.snapshot;

import org.jmouse.crawler.api.ProcessingTask;

import java.util.List;

public record FrontierSnapshot(List<ProcessingTask> tasks) {
    public static FrontierSnapshot empty() {
        return new FrontierSnapshot(List.of());
    }
}
