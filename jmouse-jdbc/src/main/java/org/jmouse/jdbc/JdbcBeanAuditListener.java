package org.jmouse.jdbc;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.events.BeanContextEventPayload;
import org.jmouse.core.events.AbstractEventListener;
import org.jmouse.core.events.Event;
import org.jmouse.core.events.annotation.Listener;

@Bean
@Listener(events = {"BEAN_CREATED", "BEAN_CREATE_FAILED", "BEAN_LOOKUP_START"})
public class JdbcBeanAuditListener extends AbstractEventListener<BeanContextEventPayload> {

    @Override
    public Class<?> payloadType() {
        return BeanContextEventPayload.class;
    }

    @Override
    public void onEvent(Event<BeanContextEventPayload> event) {
        System.out.println("EVENT=" + event.name() + " payload=" + event.payload());
    }

}
