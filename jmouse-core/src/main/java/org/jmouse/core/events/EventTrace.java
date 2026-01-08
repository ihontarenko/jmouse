package org.jmouse.core.events;

import org.jmouse.core.IdGenerator;
import org.jmouse.core.UUIDGenerator;

import java.time.Instant;
import java.util.UUID;

import static org.jmouse.core.Verify.nonNull;

/**
 * 🧬 Immutable trace metadata associated with an event.
 * <p>
 * {@code EventTrace} models a lightweight execution trace similar to
 * distributed tracing systems (e.g. spans and correlation IDs),
 * allowing events to be correlated, nested, and ordered in time.
 *
 * <h3>Concepts</h3>
 * <ul>
 *   <li><b>Correlation ID</b> – identifies a logical event flow</li>
 *   <li><b>Span ID</b> – identifies the current event span</li>
 *   <li><b>Parent Span ID</b> – identifies the parent span (if any)</li>
 *   <li><b>Depth</b> – nesting level within the event flow</li>
 *   <li><b>Timestamp</b> – moment when the trace was created</li>
 * </ul>
 *
 * <p>Instances are immutable and thread-safe.</p>
 */
public record EventTrace(
        String correlationId,
        String spanId,
        String parentSpanId,
        int depth,
        Instant timestamp
) {

    public static final IdGenerator<String, String> UUID_GENERATOR = new UUIDGenerator();
    public static final String                      PREFIX         = "jMouse_";
    public static final IdGenerator<String, String> RAND_GENERATOR = () -> PREFIX + UUID_GENERATOR.generate();

    /**
     * Creates a new root-level event trace.
     * <p>
     * Root traces start a new correlation scope and
     * have no parent span.
     *
     * @return a new root {@code EventTrace}
     */
    public static EventTrace root() {
        return new EventTrace(
                RAND_GENERATOR.generate(),
                RAND_GENERATOR.generate(),
                null,
                0,
                Instant.now()
        );
    }

    /**
     * Creates a child trace derived from this trace.
     * <p>
     * The child trace shares the same correlation ID,
     * references this trace as its parent, and increases
     * the nesting depth by one.
     *
     * @return a new child {@code EventTrace}
     */
    public EventTrace child() {
        return new EventTrace(
                correlationId,
                RAND_GENERATOR.generate(),
                spanId,
                depth + 1,
                Instant.now()
        );
    }

    /**
     * Creates a new trace instance with the same identity
     * but an updated timestamp.
     * <p>
     * Useful for signaling event progression without
     * changing span relationships.
     *
     * @return a new {@code EventTrace} with refreshed timestamp
     */
    public EventTrace touch() {
        return new EventTrace(
                correlationId,
                spanId,
                parentSpanId,
                depth,
                Instant.now()
        );
    }

    /**
     * Compact constructor enforcing mandatory fields.
     */
    public EventTrace {
        nonNull(correlationId, "correlationId");
        nonNull(spanId, "spanId");
        nonNull(timestamp, "timestamp");
    }
}
