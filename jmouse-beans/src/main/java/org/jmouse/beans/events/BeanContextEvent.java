package org.jmouse.beans.events;

import org.jmouse.core.events.AbstractEvent;
import org.jmouse.core.events.EventName;

final public class BeanContextEvent extends AbstractEvent<BeanContextEventPayload> {
    public BeanContextEvent(EventName name, BeanContextEventPayload payload, Object caller) {
        super(name, payload, caller);
    }
}
