package org.jmouse.beans.events;

import org.jmouse.core.observer.AbstractEvent;

final public class BeanContextEvent extends AbstractEvent<BeanContextEventPayload> {
    public BeanContextEvent(String name, BeanContextEventPayload payload, Object caller) {
        super(name, payload, caller);
    }
}
