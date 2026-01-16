package org.jmouse.crawler.runtime;

import java.net.URI;
import java.time.Instant;

public record ProcessingTask(
        URI url,
        int depth,
        URI parent,
        String discoveredBy,
        int priority,
        Instant scheduledAt,
        int attempt,
        RoutingHint hint
) {
    public ProcessingTask attempt(Instant now) {
        return new ProcessingTask(url, depth, parent, discoveredBy, priority, now, attempt + 1, hint);
    }

    public ProcessingTask schedule(Instant scheduledAt) {
        return new ProcessingTask(url, depth, parent, discoveredBy, priority, scheduledAt, attempt, hint);
    }

}
