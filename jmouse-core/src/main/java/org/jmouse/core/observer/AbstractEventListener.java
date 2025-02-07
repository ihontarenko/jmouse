package org.jmouse.core.observer;

import org.jmouse.util.helper.Strings;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.TypeMatchers;

abstract public class AbstractEventListener<T> implements EventListener<T> {

    protected final Matcher<Class<?>> matcher = TypeMatchers.isSimilar(applicableType());

    @Override
    public String name() {
        return Strings.underscored(getClass().getSimpleName(), true);
    }

    @Override
    public boolean supports(Class<?> actualType) {
        return matcher.matches(actualType);
    }

}
