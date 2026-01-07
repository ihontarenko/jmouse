package org.jmouse.beans.events;

import org.jmouse.core.Verify;
import org.jmouse.core.events.Event;
import org.jmouse.core.events.EventListener;
import org.jmouse.core.events.EventManager;

import java.util.function.Consumer;

import static org.jmouse.beans.events.BeanContextEventPayload.*;

/**
 * Convenience hooks API for subscribing to {@link BeanContextEventName} lifecycle events.
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
            BeanContextEventName name,
            Class<P> payloadType,
            Consumer<P> consumer
    ) {
        Verify.nonNull(name, "name");
        Verify.nonNull(payloadType, "payloadType");
        Verify.nonNull(consumer, "consumer");
        getEventManager().subscribe(name.name(), new TypedPayloadListener<>(payloadType, consumer));
        return this;
    }

    default BeanContextEventSupport onBeforeRefresh(Consumer<ContextPayload> consumer) {
        return on(BeanContextEventName.CONTEXT_REFRESH_START, ContextPayload.class, consumer);
    }

    default BeanContextEventSupport onAfterRefresh(Consumer<ContextPayload> consumer) {
        return on(BeanContextEventName.CONTEXT_REFRESH_FINISH, ContextPayload.class, consumer);
    }

    /**
     * Subscribe to the bean lookup start event.
     * <p>
     * Invoked when the context begins resolving a bean,
     * before any instantiation or caching logic is applied.
     *
     * @param consumer callback receiving {@link LookupPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeanLookupStart(Consumer<LookupPayload> consumer) {
        return on(BeanContextEventName.BEAN_LOOKUP_START, LookupPayload.class, consumer);
    }

    /**
     * Subscribe to the bean created event.
     * <p>
     * Invoked after a bean instance has been successfully created
     * and fully initialized by the context.
     *
     * @param consumer callback receiving {@link CreatePayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeanCreated(Consumer<CreatePayload> consumer) {
        return on(BeanContextEventName.BEAN_CREATED, CreatePayload.class, consumer);
    }

    /**
     * Subscribe to bean not found event.
     *
     * @param consumer callback receiving {@link LookupPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeanNotFound(Consumer<LookupPayload> consumer) {
        return on(BeanContextEventName.BEAN_NOT_FOUND, LookupPayload.class, consumer);
    }

    /**
     * Subscribe to bean found event.
     *
     * @param consumer callback receiving {@link LookupPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeanFound(Consumer<LookupPayload> consumer) {
        return on(BeanContextEventName.BEAN_FOUND, LookupPayload.class, consumer);
    }

    /**
     * Subscribe to bean creation failure event.
     *
     * @param consumer callback receiving {@link ErrorPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeanCreateFailed(Consumer<ErrorPayload> consumer) {
        return on(BeanContextEventName.BEAN_CREATE_FAILED, ErrorPayload.class, consumer);
    }

    /**
     * Subscribe to any context error event.
     *
     * @param consumer callback receiving {@link ErrorPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onContextError(Consumer<ErrorPayload> consumer) {
        return on(BeanContextEventName.CONTEXT_ERROR, ErrorPayload.class, consumer);
    }

    /**
     * Subscribe to any "general error" event.
     *
     * @param consumer callback receiving {@link ErrorPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onError(Consumer<ErrorPayload> consumer) {
        return on(BeanContextEventName.GENERAL_ERROR, ErrorPayload.class, consumer);
    }

    /**
     * Subscribe to bean post-processing stages (before/after init).
     *
     * @param consumer callback receiving {@link InitPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeanProcessedBeforeInit(Consumer<InitPayload> consumer) {
        return on(BeanContextEventName.BEAN_PROCESSED_BEFORE_INIT, InitPayload.class, consumer);
    }

    /**
     * Subscribe to bean post-processing stages (after init).
     *
     * @param consumer callback receiving {@link InitPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeanProcessedAfterInit(Consumer<InitPayload> consumer) {
        return on(BeanContextEventName.BEAN_PROCESSED_AFTER_INIT, InitPayload.class, consumer);
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
