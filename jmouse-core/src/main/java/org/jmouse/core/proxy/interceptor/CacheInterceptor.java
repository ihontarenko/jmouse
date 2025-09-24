package org.jmouse.core.proxy.interceptor;

import org.jmouse.core.cache.BasicCache;
import org.jmouse.core.cache.TTLCache;
import org.jmouse.core.proxy.MethodInterceptor;
import org.jmouse.core.proxy.MethodInvocation;

import java.lang.reflect.Method;

/**
 * 🗄️ Interceptor that caches results of method calls using a {@link BasicCache}.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Stores method return values keyed by {@link Method}</li>
 *   <li>Uses a {@link TTLCache} to automatically evict entries after a time-to-live (TTL)</li>
 *   <li>Reduces repeated expensive calls to the same method</li>
 * </ul>
 *
 * <p>⚠️ Note: this cache does not consider method arguments.
 * All calls to the same method (regardless of parameters) will share the same cached value.</p>
 */
public class CacheInterceptor implements MethodInterceptor {

    private final BasicCache<Method, Object> cache;

    /**
     * Creates a new cache interceptor with time-based eviction.
     *
     * @param ttl time-to-live in milliseconds for cached entries
     */
    public CacheInterceptor(long ttl) {
        this.cache = new TTLCache<>(ttl);
    }

    /**
     * Executes the target method with caching.
     * If the method was already executed and the result is cached (and valid),
     * the cached value is returned instead of invoking the method again.
     *
     * @param invocation method invocation
     * @return cached or freshly computed result
     * @throws Throwable if the underlying method throws an exception
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object cached = cache.get(invocation.getMethod());

        if (cached == null) {
            Object value = invocation.proceed();
            cache.set(invocation.getMethod(), value);
            cached = value;
        }

        return cached;
    }
}
