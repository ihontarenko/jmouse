package org.jmouse.core.events;

import java.time.Instant;
import java.util.UUID;

import static org.jmouse.core.Verify.nonNull;

public record EventTrace(
        String correlationId,
        String spanId,
        String parentSpanId,
        int depth,
        Instant timestamp
) {

    public static EventTrace root() {
        return new EventTrace(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                null,
                0,
                Instant.now()
        );
    }

    public EventTrace child() {
        return new EventTrace(
                correlationId,
                UUID.randomUUID().toString(),
                spanId,
                depth + 1,
                Instant.now()
        );
    }

    public EventTrace touch() {
        return new EventTrace(correlationId, spanId, parentSpanId, depth, Instant.now());
    }

    public EventTrace {
        nonNull(correlationId, "correlationId");
        nonNull(spanId, "spanId");
        nonNull(timestamp, "timestamp");
    }
}
