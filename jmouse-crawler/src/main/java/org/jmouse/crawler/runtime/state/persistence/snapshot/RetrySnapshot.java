package org.jmouse.crawler.runtime.state.persistence.snapshot;

import org.jmouse.crawler.api.ProcessingTask;

import java.time.Instant;
import java.util.List;

public record RetrySnapshot(List<Item> items) {
    public static RetrySnapshot empty() {
        return new RetrySnapshot(List.of());
    }

    public record Item(Instant eligibleAt, ProcessingTask task, String reason) {
    }
}
