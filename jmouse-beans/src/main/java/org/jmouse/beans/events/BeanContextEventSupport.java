package org.jmouse.beans.events;

import org.jmouse.core.Verify;
import org.jmouse.core.events.Event;
import org.jmouse.core.events.EventListener;
import org.jmouse.core.events.EventManager;
import org.jmouse.core.events.EventName;

import java.util.function.Consumer;

import static org.jmouse.beans.events.BeanContextEventPayload.*;

/**
 * Convenience hooks API for subscribing to {@link BeanEventName} lifecycle events.
 *
 * <p>Implement this interface in a concrete BeanContext to provide ergonomic
 * {@code onBean...} methods while delegating the underlying subscription
 * mechanics to an {@code EventManager}.</p>
 */
public interface BeanContextEventSupport {

    String TYPED_PAYLOAD_LISTENER = "typed_payload_listener";

    /**
     * Underlying event manager used to subscribe listeners.
     *
     * @return event manager instance
     */
    EventManager getEventManager();

    /**
     * Subscribe to a raw event by name.
     *
     * @param name     stable event name
     * @param consumer consumer invoked on event payload
     * @param <P>      expected payload type
     * @return this (fluent)
     */
    default <P extends BeanContextEventPayload> BeanContextEventSupport on(
            EventName name,
            Class<P> payloadType,
            Consumer<P> consumer
    ) {
        Verify.nonNull(name, "name");
        Verify.nonNull(payloadType, "payloadType");
        Verify.nonNull(consumer, "consumer");
        getEventManager().subscribe(name, new TypedPayloadListener<>(payloadType, consumer));
        return this;
    }

    default BeanContextEventSupport onBeforeRefresh(Consumer<ContextPayload> consumer) {
        return on(BeanEventName.CONTEXT_REFRESH_STARTED, ContextPayload.class, consumer);
    }

    default BeanContextEventSupport onAfterRefresh(Consumer<ContextPayload> consumer) {
        return on(BeanEventName.CONTEXT_REFRESH_COMPLETED, ContextPayload.class, consumer);
    }

    default BeanContextEventSupport onRefreshFailed(Consumer<ErrorPayload> consumer) {
        return on(BeanEventName.CONTEXT_REFRESH_FAILED, ErrorPayload.class, consumer);
    }

    default BeanContextEventSupport onBeanLookupStarted(Consumer<LookupPayload> consumer) {
        return on(BeanEventName.BEAN_LOOKUP_STARTED, LookupPayload.class, consumer);
    }

    default BeanContextEventSupport onBeanLookupResolved(Consumer<LookupPayload> consumer) {
        return on(BeanEventName.BEAN_LOOKUP_RESOLVED, LookupPayload.class, consumer);
    }

    default BeanContextEventSupport onBeanLookupNotFound(Consumer<LookupPayload> consumer) {
        return on(BeanEventName.BEAN_LOOKUP_NOT_FOUND, LookupPayload.class, consumer);
    }

    default BeanContextEventSupport onBeanCreationStarted(Consumer<CreatePayload> consumer) {
        return on(BeanEventName.BEAN_CREATION_STARTED, CreatePayload.class, consumer);
    }

    default BeanContextEventSupport onBeanCreationCompleted(Consumer<CreatePayload> consumer) {
        return on(BeanEventName.BEAN_CREATION_COMPLETED, CreatePayload.class, consumer);
    }

    default BeanContextEventSupport onBeanCreationFailed(Consumer<ErrorPayload> consumer) {
        return on(BeanEventName.BEAN_CREATION_FAILED, ErrorPayload.class, consumer);
    }

    default BeanContextEventSupport onContextInternalError(Consumer<ErrorPayload> consumer) {
        return on(BeanEventName.CONTEXT_INTERNAL_ERROR, ErrorPayload.class, consumer);
    }

    /**
     * Internal adapter that casts payload and delegates to a consumer.
     *
     * @param <P> payload type
     */
    final class TypedPayloadListener<P extends BeanContextEventPayload> implements EventListener<BeanContextEventPayload> {

        private final Class<P> payloadType;
        private final Consumer<P> consumer;

        public TypedPayloadListener(Class<P> payloadType, Consumer<P> consumer) {
            this.payloadType = Verify.nonNull(payloadType, "payloadType");
            this.consumer = Verify.nonNull(consumer, "consumer");
        }

        @Override
        public String name() {
            return TYPED_PAYLOAD_LISTENER;
        }

        @Override
        public void onEvent(Event<BeanContextEventPayload> event) {
            BeanContextEventPayload payload = event.payload();
            if (payloadType.isInstance(payload)) {
                consumer.accept(payloadType.cast(payload));
            }
        }

        @Override
        public Class<?> payloadType() {
            return BeanContextEventPayload.class;
        }

        @Override
        public boolean supportsPayloadType(Class<?> actualType) {
            return payloadType().isAssignableFrom(actualType);
        }
    }
}
