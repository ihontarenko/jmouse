package org.jmouse.core.context.keys;

import org.jmouse.core.context.ContextKey;

public interface MutableContextKeyRegistry extends ContextKeyRegistry {

    MutableContextKeyRegistry registerForUserClass(Class<?> userClass, ContextKey<?> key);

    MutableContextKeyRegistry registerForValue(Object value, ContextKey<?> key);

}
