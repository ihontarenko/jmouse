package org.jmouse.core.limits;

import org.jmouse.core.proxy.MethodInterceptor;
import org.jmouse.core.proxy.ProxyContext;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intercepts method calls and enforces @RateLimit using token buckets.
 */
public final class RateLimitMethodInterceptor implements MethodInterceptor {

    private final RateLimitKeyResolver        keyResolver; // may be null
    private final Map<BucketKey, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitMethodInterceptor() {
        this(null);
    }

    public RateLimitMethodInterceptor(RateLimitKeyResolver keyResolver) {
        this.keyResolver = keyResolver;
    }

    private static String methodSignature(Method method) {
        StringBuilder builder        = new StringBuilder(Reflections.getMethodName(method));
        Parameter[]   parameters = method.getParameters();

        builder.append('(');

        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            Reflections.getParameterName(parameters[i]);
        }

        return builder.append(')').toString();
    }

    private static long toNanos(ChronoUnit unit) {
        return switch (unit) {
            case NANOS -> 1L;
            case MICROS -> 1_000L;
            case MILLIS -> 1_000_000L;
            case SECONDS -> 1_000_000_000L;
            case MINUTES -> 60L * 1_000_000_000L;
            case HOURS -> 3600L * 1_000_000_000L;
            case DAYS -> 24L * toNanos(ChronoUnit.HOURS);
            default -> throw new IllegalArgumentException(
                    "UNSUPPORTED UNIT: %s".formatted(unit));
        };
    }

    @Override
    public void before(ProxyContext context, Method method, Object[] arguments) {
        RateLimit annotation = resolveConfig(method, context.getTarget().getClass());
        if (annotation == null) return; // no limit

        BucketKey   key    = keyOf(annotation, context.getTarget(), method, arguments);
        TokenBucket bucket = buckets.computeIfAbsent(key, k
                -> new TokenBucket(annotation.max(), toNanos(annotation.per())));

        if (!bucket.tryAcquire()) {
            String where = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
            String name  = annotation.name().isEmpty() ? "" : ("[%s] ".formatted(annotation.name()));
            throw new RateLimitExceededException(
                    "Rate limit %s exceeded for %s (max %d per %s)".formatted(name, where, annotation.max(), annotation.per()
                            .toString().toLowerCase()));
        }
    }

    private RateLimit resolveConfig(Method m, Class<?> targetClass) {
        RateLimit a = m.getAnnotation(RateLimit.class);
        if (a != null) return a;
        return targetClass.getAnnotation(RateLimit.class);
    }

    private BucketKey keyOf(RateLimit rateLimit, Object target, Method method, Object[] args) {
        return switch (rateLimit.scope()) {
            case METHOD -> new BucketKey("M", methodSignature(method), rateLimit.name());
            case CLASS -> new BucketKey("C", target.getClass().getName(), rateLimit.name());
            case TARGET -> new BucketKey("T", System.identityHashCode(target), rateLimit.name());
            case CUSTOM -> new BucketKey("X", keyResolver.resolve(target, method, args), rateLimit.name());
        };
    }

    /**
     * Composite key for bucket map
     */
    private record BucketKey(String scope, Object id, String name) {
        @Override
        public String toString() {
            return "%s:%s:%s".formatted(scope, id, name);
        }
    }
}
