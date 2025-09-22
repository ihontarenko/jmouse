package org.jmouse.context.proxy.interceptors;

import org.jmouse.context.proxy.api.MethodInterceptor;
import org.jmouse.context.proxy.api.MethodInvocation;

import java.time.Clock;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple memoization cache with TTL by a computed key.
 * NOTE: cache only for pure methods; equals/hashCode for args must be stable.
 */
public final class CacheInterceptor implements MethodInterceptor {

    public interface KeyResolver {
        Object resolve(MethodInvocation inv);
        static KeyResolver methodAndArgs() {
            return inv -> new Key(inv.getMethod(), Arrays.deepHashCode(inv.getArguments()));
        }
        record Key(Object m, int hash) {}
    }

    private static final class Entry {
        final Object value;
        final long   expiresAtNanos;
        Entry(Object v, long ttlNanos, long now) {
            this.value = v; this.expiresAtNanos = now + ttlNanos;
        }
        boolean expired(long now) { return now >= expiresAtNanos; }
    }

    private final Map<Object, Entry> map = new ConcurrentHashMap<>();
    private final KeyResolver keyResolver;
    private final long ttlNanos;
    private final Clock clock;

    public CacheInterceptor(KeyResolver resolver, long ttlMillis, Clock clock) {
        this.keyResolver = Objects.requireNonNull(resolver);
        this.ttlNanos = ttlMillis * 1_000_000L;
        this.clock = clock;
    }

    @Override
    public Object invoke(MethodInvocation inv) throws Throwable {
        Object key = keyResolver.resolve(inv);
        long now = nowNanos();
        Entry e = map.get(key);
        if (e != null && !e.expired(now)) return e.value;

        Object v = inv.proceed();
        map.put(key, new Entry(v, ttlNanos, now));
        return v;
    }

    private long nowNanos() {
        var inst = clock.instant();
        return inst.getEpochSecond() * 1_000_000_000L + inst.getNano();
    }
}
