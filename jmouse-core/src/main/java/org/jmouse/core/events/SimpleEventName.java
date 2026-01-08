package org.jmouse.core.events;

import static org.jmouse.core.Verify.nonNull;

/**
 * Simple {@link EventName} implementation backed by strings.
 */
public record SimpleEventName(String id, EventCategory category) implements EventName {

    public SimpleEventName(String id, EventCategory category) {
        this.id = nonNull(id, "id");
        this.category = nonNull(category, "category");
    }

    @Override
    public String toString() {
        return id;
    }

}
