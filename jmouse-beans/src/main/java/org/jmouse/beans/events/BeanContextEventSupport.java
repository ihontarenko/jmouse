package org.jmouse.beans.events;

import org.jmouse.core.Verify;
import org.jmouse.core.events.Event;
import org.jmouse.core.events.EventListener;
import org.jmouse.core.events.EventManager;
import org.jmouse.core.events.EventName;
import org.jmouse.util.Strings;

import java.util.function.Consumer;

import static org.jmouse.beans.events.BeanContextEventPayload.*;

/**
 * Convenience hooks API for subscribing to {@link BeanEventName} lifecycle events. 🧩
 *
 * <p>Implement this interface in a BeanContext (or any component that owns an {@link EventManager})
 * to expose ergonomic {@code on...} methods while delegating the actual subscription mechanism
 * to the event manager.</p>
 *
 * <p>All hook methods are fluent and return {@code this}.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public final class DefaultBeanContext implements BeanContextEventSupport {
 *     private final EventManager events = new DefaultEventManager();
 *
 *     @Override
 *     public EventManager getEventManager() {
 *         return events;
 *     }
 *
 *     public void wireHooks() {
 *         onBeforeRefresh(ctx -> System.out.println("Refresh started: " + ctx));
 *         onBeanCreationFailed(err -> System.err.println("Bean failed: " + err));
 *     }
 * }
 * }</pre>
 *
 * @see EventManager
 * @see BeanEventName
 */
public interface BeanContextEventSupport {

    /**
     * Underlying event manager used to subscribe listeners.
     *
     * @return event manager instance
     */
    EventManager getEventManager();

    /**
     * Subscribe to a raw event by name with a payload type guard.
     *
     * <p>The provided {@code consumer} is invoked only when the event payload is an instance
     * of {@code payloadType}.</p>
     *
     * @param name stable event name
     * @param payloadType expected payload type
     * @param consumer consumer invoked on matching payload
     * @param <P> expected payload type
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

    /**
     * Subscribe to {@link BeanEventName#CONTEXT_REFRESH_STARTED}.
     *
     * @param consumer callback for {@link ContextPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeforeRefresh(Consumer<ContextPayload> consumer) {
        return on(BeanEventName.CONTEXT_REFRESH_STARTED, ContextPayload.class, consumer);
    }

    /**
     * Subscribe to {@link BeanEventName#CONTEXT_REFRESH_COMPLETED}.
     *
     * @param consumer callback for {@link ContextPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onAfterRefresh(Consumer<ContextPayload> consumer) {
        return on(BeanEventName.CONTEXT_REFRESH_COMPLETED, ContextPayload.class, consumer);
    }

    /**
     * Subscribe to {@link BeanEventName#CONTEXT_REFRESH_FAILED}.
     *
     * @param consumer callback for {@link ErrorPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onRefreshFailed(Consumer<ErrorPayload> consumer) {
        return on(BeanEventName.CONTEXT_REFRESH_FAILED, ErrorPayload.class, consumer);
    }

    /**
     * Subscribe to {@link BeanEventName#BEAN_LOOKUP_STARTED}.
     *
     * @param consumer callback for {@link LookupPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeanLookupStarted(Consumer<LookupPayload> consumer) {
        return on(BeanEventName.BEAN_LOOKUP_STARTED, LookupPayload.class, consumer);
    }

    /**
     * Subscribe to {@link BeanEventName#BEAN_LOOKUP_RESOLVED}.
     *
     * @param consumer callback for {@link LookupPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeanLookupResolved(Consumer<LookupPayload> consumer) {
        return on(BeanEventName.BEAN_LOOKUP_RESOLVED, LookupPayload.class, consumer);
    }

    /**
     * Subscribe to {@link BeanEventName#BEAN_LOOKUP_NOT_FOUND}.
     *
     * @param consumer callback for {@link LookupPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeanLookupNotFound(Consumer<LookupPayload> consumer) {
        return on(BeanEventName.BEAN_LOOKUP_NOT_FOUND, LookupPayload.class, consumer);
    }

    /**
     * Subscribe to {@link BeanEventName#BEAN_CREATION_STARTED}.
     *
     * @param consumer callback for {@link CreatePayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeanCreationStarted(Consumer<CreatePayload> consumer) {
        return on(BeanEventName.BEAN_CREATION_STARTED, CreatePayload.class, consumer);
    }

    /**
     * Subscribe to {@link BeanEventName#BEAN_CREATION_COMPLETED}.
     *
     * @param consumer callback for {@link CreatePayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeanCreationCompleted(Consumer<CreatePayload> consumer) {
        return on(BeanEventName.BEAN_CREATION_COMPLETED, CreatePayload.class, consumer);
    }

    /**
     * Subscribe to {@link BeanEventName#BEAN_CREATION_FAILED}.
     *
     * @param consumer callback for {@link ErrorPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onBeanCreationFailed(Consumer<ErrorPayload> consumer) {
        return on(BeanEventName.BEAN_CREATION_FAILED, ErrorPayload.class, consumer);
    }

    /**
     * Subscribe to {@link BeanEventName#CONTEXT_INTERNAL_ERROR}.
     *
     * @param consumer callback for {@link ErrorPayload}
     * @return this (fluent)
     */
    default BeanContextEventSupport onContextInternalError(Consumer<ErrorPayload> consumer) {
        return on(BeanEventName.CONTEXT_INTERNAL_ERROR, ErrorPayload.class, consumer);
    }

    /**
     * Internal adapter that performs a runtime payload type check and delegates to a consumer. 🔌
     *
     * @param <P> payload type
     */
    final class TypedPayloadListener<P extends BeanContextEventPayload>
            implements EventListener<BeanContextEventPayload> {

        private final Class<P> payloadType;
        private final Consumer<P> consumer;

        /**
         * Create a listener that delegates to {@code consumer} for payloads of {@code payloadType}.
         *
         * @param payloadType payload type guard
         * @param consumer payload consumer
         */
        public TypedPayloadListener(Class<P> payloadType, Consumer<P> consumer) {
            this.payloadType = Verify.nonNull(payloadType, "payloadType");
            this.consumer = Verify.nonNull(consumer, "consumer");
        }

        /**
         * Listener name used for registration/diagnostics.
         *
         * @return underscored listener name
         */
        @Override
        public String name() {
            return Strings.underscored(getClass().getSimpleName(), true);
        }

        /**
         * Handle an event by type-checking the payload and invoking the consumer.
         *
         * @param event incoming event
         */
        @Override
        public void onEvent(Event<BeanContextEventPayload> event) {
            BeanContextEventPayload payload = event.payload();
            if (payloadType.isInstance(payload)) {
                consumer.accept(payloadType.cast(payload));
            }
        }

        /**
         * Declared payload base type for this listener.
         *
         * @return base payload type
         */
        @Override
        public Class<?> payloadType() {
            return BeanContextEventPayload.class;
        }

        /**
         * Whether this listener supports the provided payload type.
         *
         * @param actualType actual payload type
         * @return {@code true} if assignable, otherwise {@code false}
         */
        @Override
        public boolean supportsPayloadType(Class<?> actualType) {
            return payloadType().isAssignableFrom(actualType);
        }
    }
}
