package org.jmouse.crawler.runtime.state.persistence.dto;

import java.net.URI;
import java.time.Instant;

/**
 * Persistence DTO for ProcessingTask and its nested aggregates.
 *
 * <p>Intentionally concrete (no polymorphism) to keep codecs decoupled from runtime model.</p>
 */
public record ProcessingTaskDto(
        String id,
        TraceContextDto trace,
        URI url,
        int depth,
        URI parent,
        TaskOriginDto origin,
        int priority,
        Instant scheduledAt,
        int attempt,
        String hint
) {}