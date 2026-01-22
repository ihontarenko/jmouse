package org.jmouse.crawler.runtime.state.persistence.dto;

import java.time.Instant;

/** DTO for TraceContext. */
public record TraceContextDto(
        String correlationId,
        String spanId,
        String parentSpanId,
        int depth,
        Instant timestamp
) {}