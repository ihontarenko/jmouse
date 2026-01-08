package org.jmouse.core.events;

public record AnyEvent(EventCategory category) implements EventName {
    @Override
    public String id() {
        return "*";
    }
}
