package org.jmouse.crawler.runtime;

import java.net.URI;
import java.time.Instant;

public record CrawlTask(
        URI url,
        int depth,
        URI parent,
        String discoveredBy,
        int priority,
        Instant scheduledAt,
        int attempt,
        CrawlHint hint
) {
    public CrawlTask attempt(Instant now) {
        return new CrawlTask(url, depth, parent, discoveredBy, priority, now, attempt + 1, hint);
    }

    public CrawlTask schedule(Instant scheduledAt) {
        return new CrawlTask(url, depth, parent, discoveredBy, priority, scheduledAt, attempt, hint);
    }

}
