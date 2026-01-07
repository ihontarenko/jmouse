package org.jmouse.jdbc;

import org.jmouse.beans.events.BeanContextEventPayload;
import org.jmouse.core.observer.AbstractEventListener;
import org.jmouse.core.observer.Event;
import org.jmouse.core.observer.annotation.Listener;

@Listener(events = {"BEAN_CREATED", "BEAN_CREATE_FAILED"})
public class JdbcBeanAuditListener extends AbstractEventListener<BeanContextEventPayload> {

    @Override
    public Class<?> applicableType() {
        return BeanContextEventPayload.class;
    }

    @Override
    public void update(Event<BeanContextEventPayload> event) {
        System.out.println("EVENT=" + event.name() + " payload=" + event.payload());
    }

}
