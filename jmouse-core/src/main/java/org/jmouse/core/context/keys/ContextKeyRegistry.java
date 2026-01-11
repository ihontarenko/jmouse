package org.jmouse.core.context.keys;

import org.jmouse.core.Verify;
import org.jmouse.core.context.ContextKey;

import java.util.Map;

public interface ContextKeyRegistry {

    /**
     * Return a canonical {@link ContextKey} instance for the given value,
     * or {@code null} when no key is registered.
     */
    ContextKey<?> findKeyForValue(Object value);

    /**
     * Return a canonical {@link ContextKey} instance for the given user class,
     * or {@code null} when no key is registered.
     */
    ContextKey<?> findKeyForUserClass(Class<?> userClass);

    /**
     * Default registry with no pre-registered keys.
     * Always returns {@code null}.
     */
    static ContextKeyRegistry noop() {
        return NoopContextKeyRegistry.INSTANCE;
    }

    /**
     * Create an empty mutable registry.
     */
    static MutableContextKeyRegistry mutable() {
        return MapBackedContextKeyRegistry.create();
    }

    /**
     * Create a registry backed by a user-class mapping.
     */
    static ContextKeyRegistry ofUserClassMappings(Map<Class<?>, ContextKey<?>> mappings) {
        Verify.nonNull(mappings, "mappings");
        return new UserClassOnlyContextKeyRegistry(mappings);
    }

    /**
     * Create a registry backed by identity-based value mapping.
     */
    static ContextKeyRegistry ofValueMappingsIdentity(Map<Object, ContextKey<?>> mappings) {
        Verify.nonNull(mappings, "mappings");
        return new IdentityValueOnlyContextKeyRegistry(mappings);
    }

    /**
     * Create a composite registry that queries registries in order.
     * The first non-null key wins.
     */
    static ContextKeyRegistry composite(ContextKeyRegistry... registries) {
        Verify.nonNull(registries, "registries");
        return new CompositeContextKeyRegistry(registries);
    }
}
