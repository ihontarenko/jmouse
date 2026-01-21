package org.jmouse.crawler.api;

import java.time.Instant;

public record DeadLetterItem(
        Instant failedAt,
        String reason,
        String stageId,
        String routeId,
        int attempt,
        Throwable error
) {}
