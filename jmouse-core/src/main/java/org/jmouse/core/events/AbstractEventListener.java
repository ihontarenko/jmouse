package org.jmouse.core.events;

import org.jmouse.util.Strings;

abstract public class AbstractEventListener<T> implements EventListener<T> {

    @Override
    public String name() {
        return Strings.underscored(getClass().getSimpleName(), true);
    }

    @Override
    public boolean supportsPayloadType(Class<?> actualType) {
        return payloadType().isAssignableFrom(actualType);
    }

}
