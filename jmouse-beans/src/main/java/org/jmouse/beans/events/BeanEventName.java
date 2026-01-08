package org.jmouse.beans.events;

import org.jmouse.core.events.EventCategory;
import org.jmouse.core.events.EventName;

/**
 * Defines all lifecycle, resolution, and failure events emitted by a {@code BeanContext}.
 */
public enum BeanEventName implements EventName {

    CONTEXT_REFRESH_STARTED(
            "bean.context.refresh.started",
            "Refresh started",
            BeanEventCategory.CONTEXT
    ),
    CONTEXT_REFRESH_COMPLETED(
            "bean.context.refresh.completed",
            "Refresh completed",
            BeanEventCategory.CONTEXT
    ),
    CONTEXT_REFRESH_FAILED(
            "bean.context.refresh.failed",
            "Refresh failed",
            BeanEventCategory.CONTEXT
    ),

    BEAN_DEFINITION_REGISTERED(
            "bean.definition.registered",
            "Definition registered",
            BeanEventCategory.DEFINITION
    ),
    BEAN_DEFINITION_REGISTRATION_FAILED(
            "bean.definition.registration.failed",
            "Definition registration failed",
            BeanEventCategory.DEFINITION
    ),

    BEAN_LOOKUP_STARTED(
            "bean.lookup.started",
            "Lookup started",
            BeanEventCategory.LOOKUP
    ),
    BEAN_LOOKUP_RESOLVED(
            "bean.lookup.resolved",
            "Lookup resolved",
            BeanEventCategory.LOOKUP
    ),
    BEAN_LOOKUP_NOT_FOUND(
            "bean.lookup.not_found",
            "Bean not found",
            BeanEventCategory.LOOKUP
    ),
    BEAN_LOOKUP_AMBIGUOUS(
            "bean.lookup.ambiguous",
            "Lookup ambiguous",
            BeanEventCategory.LOOKUP
    ),
    BEAN_LOOKUP_PRIMARY_SELECTED(
            "bean.lookup.primary_selected",
            "Primary selected",
            BeanEventCategory.LOOKUP
    ),

    BEAN_CREATION_STARTED(
            "bean.creation.started",
            "Creation started",
            BeanEventCategory.CREATION
    ),
    BEAN_CREATION_COMPLETED(
            "bean.creation.completed",
            "Creation completed",
            BeanEventCategory.CREATION
    ),
    BEAN_CREATION_FAILED(
            "bean.creation.failed",
            "Creation failed",
            BeanEventCategory.CREATION
    ),

    BEAN_INITIALIZATION_STARTED(
            "bean.initialization.started",
            "Initialization started",
            BeanEventCategory.INITIALIZATION
    ),
    BEAN_INITIALIZATION_BEFORE_PROCESSING(
            "bean.initialization.before_processing",
            "Before processing",
            BeanEventCategory.INITIALIZATION
    ),
    BEAN_INITIALIZER_INVOKED(
            "bean.initialization.initializer_invoked",
            "Initializer invoked",
            BeanEventCategory.INITIALIZATION
    ),
    BEAN_INITIALIZATION_AFTER_PROCESSING(
            "bean.initialization.after_processing",
            "After processing",
            BeanEventCategory.INITIALIZATION
    ),
    BEAN_INITIALIZATION_COMPLETED(
            "bean.initialization.completed",
            "Initialization completed",
            BeanEventCategory.INITIALIZATION
    ),
    BEAN_INITIALIZATION_FAILED(
            "bean.initialization.failed",
            "Initialization failed",
            BeanEventCategory.INITIALIZATION
    ),

    CONTEXT_INTERNAL_ERROR(
            "bean.internal.error",
            "Internal error",
            BeanEventCategory.INTERNAL
    );

    private final String id;
    private final String label;
    private final BeanEventCategory category;

    BeanEventName(String id, String label, BeanEventCategory category) {
        this.id = id;
        this.label = label;
        this.category = category;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public EventCategory category() {
        return category;
    }
}
