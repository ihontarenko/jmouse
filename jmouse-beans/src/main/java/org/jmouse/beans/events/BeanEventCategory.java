package org.jmouse.beans.events;

import org.jmouse.core.events.EventCategory;

/**
 * Bean module event categories.
 *
 * <p>
 * Each category provides:
 * <ul>
 *   <li>a stable {@link #id()} for routing and metrics</li>
 *   <li>a human-readable {@link #label()} for logs and UI</li>
 * </ul>
 * </p>
 */
public enum BeanEventCategory implements EventCategory {

    /**
     * BeanContext lifecycle and refresh events.
     */
    CONTEXT("bean.context", "Bean context lifecycle"),

    /**
     * Bean definition registration and validation events.
     */
    DEFINITION("bean.definition", "Bean definition management"),

    /**
     * Bean lookup and resolution events.
     */
    LOOKUP("bean.lookup", "Bean lookup and resolution"),

    /**
     * Bean instantiation and construction events.
     */
    CREATION("bean.creation", "Bean creation"),

    /**
     * Bean initialization and post-processing events.
     */
    INITIALIZATION("bean.initialization", "Bean initialization"),

    /**
     * Internal or technical events not exposed as part of normal lifecycle.
     */
    INTERNAL("bean.internal", "Internal context events");

    private final String id;
    private final String label;

    BeanEventCategory(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String id() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String label() {
        return label;
    }
}
