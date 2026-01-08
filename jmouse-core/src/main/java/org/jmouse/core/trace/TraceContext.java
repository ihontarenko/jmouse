package org.jmouse.core.trace;

import org.jmouse.core.IdGenerator;
import org.jmouse.core.SecureRandomStringGenerator;

import java.time.Instant;

import static org.jmouse.core.Verify.nonNull;

public record TraceContext(
        String correlationId,
        String spanId,
        String parentSpanId,
        int depth,
        Instant timestamp
) {

    public static final IdGenerator<String, String> STRING_GENERATOR = new SecureRandomStringGenerator(12);
    public static final String                      PREFIX           = "jMouse_";
    public static final IdGenerator<String, String> ID_GENERATOR     = () -> PREFIX + STRING_GENERATOR.generate();

    public static TraceContext root() {
        return new TraceContext(ID_GENERATOR.generate(), ID_GENERATOR.generate(), null, 0, Instant.now());
    }

    public TraceContext child() {
        return new TraceContext(correlationId, ID_GENERATOR.generate(), spanId, depth + 1, Instant.now());
    }

    public TraceContext touch() {
        return new TraceContext(correlationId, spanId, parentSpanId, depth, Instant.now());
    }

    public TraceContext {
        nonNull(correlationId, "correlationId");
        nonNull(spanId, "spanId");
        nonNull(timestamp, "timestamp");
    }

}
