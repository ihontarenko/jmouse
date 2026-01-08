package org.jmouse.beans.events;

import org.jmouse.core.events.Event;
import org.jmouse.core.events.EventListener;
import org.jmouse.core.events.EventPublishPolicy;
import org.jmouse.core.events.TraceableEvent;

import java.util.function.Consumer;

import static org.jmouse.core.Verify.nonNull;

/**
 * 🧹 Listener decorator that forwards only root-level traced events.
 * <p>
 * This listener checks {@link TraceableEvent#trace()} and invokes the delegate
 * only when the event trace is considered "root" (i.e. {@code depth == 0}).
 * <p>
 * Non-traceable events are ignored.
 *
 * @param <P> event payload type
 */
public final class RootOnlyListener<P> implements EventListener<P> {

    private static final EventPublishPolicy ROOT_ONLY = EventPublishPolicy.rootOnly();

    private final Class<P>           payloadType;
    private final Consumer<Event<P>> delegate;

    /**
     * Creates a root-only listener.
     *
     * @param payloadType expected payload type
     * @param delegate    delegate consumer invoked for root-level events
     */
    public RootOnlyListener(Class<P> payloadType, Consumer<Event<P>> delegate) {
        this.payloadType = nonNull(payloadType, "payloadType");
        this.delegate = nonNull(delegate, "delegate");
    }

    @Override
    public String name() {
        return "root_only";
    }

    @Override
    public void onEvent(Event<P> event) {
        if (event instanceof TraceableEvent<?> traceable) {
            if (ROOT_ONLY.shouldPublish(event.name(), traceable.trace(), event.caller())) {
                delegate.accept(event);
            }
        }
    }

    @Override
    public Class<?> payloadType() {
        return payloadType;
    }

    @Override
    public boolean supportsPayloadType(Class<?> actualType) {
        return payloadType.isAssignableFrom(actualType);
    }
}
