package org.jmouse.core.throttle;

import java.lang.reflect.Method;

/**
 * Resolves a key used to identify a rate limit bucket.
 */
public interface RateLimitKeyResolver {

    /**
     * @return non-null key; different keys map to independent buckets.
     */
    Object resolve(Object target, Method method, Object[] arguments);
}