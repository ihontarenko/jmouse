package org.jmouse.core.context.keys;

import org.jmouse.core.Verify;
import org.jmouse.core.reflection.Reflections;

/**
 * User-class based context key resolver.
 */
public final class UserClassContextKeyResolver implements ContextKeyResolver {

    @Override
    public Object resolveKeyFor(Object value) {
        Verify.nonNull(value, "value");
        return Reflections.getUserClass(value.getClass());
    }
}
