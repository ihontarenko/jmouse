package org.jmouse.crawler.core;

import java.net.URI;
import java.time.Instant;

public record CrawlTask(
        URI url,
        int depth,
        URI parent,
        String discoveredBy,
        int priority,
        Instant scheduledAt,
        int attempt
) {
    public CrawlTask nextAttempt(Instant now) {
        return new CrawlTask(url, depth, parent, discoveredBy, priority, now, attempt + 1);
    }
}
