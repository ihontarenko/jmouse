package org.jmouse.core.context.keys;

import org.jmouse.core.Verify;
import org.jmouse.core.reflection.Reflections;

/**
 * 👤 User-class based context key resolver.
 *
 * <p>
 * Resolves context keys using the <b>user class</b>
 * of a given value, unwrapping proxies and
 * framework-generated subclasses.
 * </p>
 *
 * <p>
 * Useful when values are proxied or enhanced
 * and the logical type should be used as a key.
 * </p>
 */
public final class UserClassContextKeyResolver implements ContextKeyResolver {

    /**
     * 🔍 Resolve key using user class.
     *
     * @param value source value
     * @return user class of the value
     */
    @Override
    public Object resolveKeyFor(Object value) {
        Verify.nonNull(value, "value");
        return Reflections.getUserClass(value.getClass());
    }
}
