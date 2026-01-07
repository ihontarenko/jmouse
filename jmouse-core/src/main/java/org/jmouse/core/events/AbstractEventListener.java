package org.jmouse.core.events;

import org.jmouse.util.Strings;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.TypeMatchers;

abstract public class AbstractEventListener<T> implements EventListener<T> {

    protected final Matcher<Class<?>> matcher = TypeMatchers.isSimilar(payloadType());

    @Override
    public String name() {
        return Strings.underscored(getClass().getSimpleName(), true);
    }

    @Override
    public boolean supportsPayloadType(Class<?> actualType) {
        return matcher.matches(actualType);
    }

}
