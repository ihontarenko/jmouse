package org.jmouse.beans.events;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;

import java.util.List;

import static org.jmouse.beans.events.BeanContextEventPayload.*;

/**
 * 🔔 Marker interface for all {@link BeanContext} event payloads.
 * <p>
 * Each implementation represents contextual data associated with
 * a specific {@link BeanContextEventName} emitted during bean
 * resolution, creation, initialization, or error handling.
 *
 * <h3>Design</h3>
 * <ul>
 *   <li>Implemented as a sealed interface for exhaustiveness.</li>
 *   <li>All payloads are immutable {@code record}s.</li>
 *   <li>Every payload carries a reference to the originating {@link BeanContext}.</li>
 * </ul>
 */
public sealed interface BeanContextEventPayload
        permits
        AmbiguousLookupPayload,
        ContextPayload,
        CreatePayload,
        DefinitionPayload,
        ErrorPayload,
        InitPayload,
        LookupPayload {

    /**
     * 🧩 Generic context-only payload.
     * <p>
     * Used for events that relate to the context lifecycle itself
     * and do not require additional metadata.
     *
     * @param context the originating bean context
     */
    record ContextPayload(BeanContext context)
            implements BeanContextEventPayload { }

    /**
     * 📘 Payload associated with a {@link BeanDefinition}.
     * <p>
     * Emitted when a bean definition becomes relevant in the
     * resolution or creation process.
     *
     * @param context     the originating bean context
     * @param definition  the involved bean definition
     */
    record DefinitionPayload(
            BeanContext context,
            BeanDefinition definition
    ) implements BeanContextEventPayload { }

    /**
     * 🔍 Payload emitted during bean lookup.
     * <p>
     * Represents an attempt to resolve a bean by name
     * and/or required type.
     *
     * @param context       the originating bean context
     * @param beanName      the requested bean name (may be {@code null})
     * @param requiredType  the required bean type (may be {@code null})
     */
    record LookupPayload(
            BeanContext context,
            String beanName,
            Class<?> requiredType
    ) implements BeanContextEventPayload { }

    /**
     * 🏗 Payload emitted after a bean instance is created.
     * <p>
     * Indicates that instantiation has completed successfully.
     *
     * @param context     the originating bean context
     * @param definition  the bean definition used
     * @param instance    the created bean instance
     */
    record CreatePayload(
            BeanContext context,
            BeanDefinition definition,
            Object instance
    ) implements BeanContextEventPayload { }

    /**
     * ⚙️ Payload emitted during bean initialization.
     * <p>
     * Typically used to signal intermediate lifecycle steps
     * such as post-processing or custom initialization hooks.
     *
     * @param context     the originating bean context
     * @param definition  the bean definition
     * @param instance    the bean instance
     * @param step        logical initialization step identifier
     */
    record InitPayload(
            BeanContext context,
            BeanDefinition definition,
            Object instance,
            String step
    ) implements BeanContextEventPayload { }

    /**
     * ❌ Payload emitted when an error occurs during a bean lifecycle stage.
     *
     * @param context     the originating bean context
     * @param stage       the lifecycle stage where the error occurred
     * @param beanName    the bean name involved (may be {@code null})
     * @param definition  the bean definition involved (may be {@code null})
     * @param error       the raised exception or error
     */
    record ErrorPayload(
            BeanContext context,
            BeanContextEventName stage,
            String beanName,
            BeanDefinition definition,
            Throwable error
    ) implements BeanContextEventPayload { }

    /**
     * ⚠️ Payload emitted when bean lookup results are ambiguous.
     * <p>
     * Indicates that multiple candidate beans match the required type
     * and no single bean could be selected.
     *
     * @param context       the originating bean context
     * @param requiredType  the required bean type
     * @param candidates    names of matching candidate beans
     */
    record AmbiguousLookupPayload(
            BeanContext context,
            Class<?> requiredType,
            List<String> candidates
    ) implements BeanContextEventPayload {}

}
