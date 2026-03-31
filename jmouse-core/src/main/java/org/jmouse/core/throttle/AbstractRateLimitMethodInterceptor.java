package org.jmouse.core.throttle;

import org.jmouse.core.proxy.InvocationContext;
import org.jmouse.core.proxy.MethodInterceptor;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.TimeHelper;

import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base interceptor for {@link RateLimit}. 🚦
 *
 * <p>Checks rate limits before method invocation.</p>
 */
public abstract class AbstractRateLimitMethodInterceptor implements MethodInterceptor {

    /**
     * Resolver used for {@link RateLimit.Scope#CUSTOM} keys.
     */
    private final RateLimitKeyResolver keyResolver;

    /**
     * Active rate-limit buckets.
     */
    private final Map<BucketKey, RateLimiter> buckets = new ConcurrentHashMap<>();

    /**
     * Creates interceptor with default custom key resolver.
     */
    public AbstractRateLimitMethodInterceptor() {
        this(new MethodArgumentsHashKeyResolver());
    }

    /**
     * Creates interceptor with custom key resolver.
     *
     * @param keyResolver custom bucket key resolver
     */
    public AbstractRateLimitMethodInterceptor(RateLimitKeyResolver keyResolver) {
        this.keyResolver = keyResolver;
    }

    /**
     * Applies rate-limit check before method execution.
     *
     * @param context   invocation context
     * @param method    invoked method
     * @param arguments invocation arguments
     *
     * @throws RateLimitExceededException when limit is exceeded
     */
    @Override
    public void before(InvocationContext context, Method method, Object[] arguments) {
        if (isSupportedType(context.target().getClass()) || isSupportedMethod(method)) {
            RateLimit              annotation = resolveAnnotation(method, context.target().getClass());
            RateLimitConfiguration config     = toConfiguration(annotation);
            BucketKey              key        = keyOf(config, context.target(), method, arguments);
            RateLimiter            limiter    = buckets.computeIfAbsent(
                    key, k -> RateLimiter.smooth(config.max(), config.periodNanos()));

            if (!limiter.tryAcquire()) {
                String name  = config.name().isBlank() ? "" : ("[%s] ".formatted(config.name()));
                String where = Reflections.getMethodName(method);
                throw new RateLimitExceededException(
                        "Rate limit %s exceeded for %s (max %d per %d %s)".formatted(
                                name, where, config.max(), config.amount(), config.per().name()), config);
            }
        }
    }

    /**
     * Checks whether the target type supports rate limiting.
     *
     * @param type target type
     * @return {@code true} if supported
     */
    private boolean isSupportedType(Class<?> type) {
        return RateLimited.class.isAssignableFrom(type)
                || type.isAnnotationPresent(RateLimitEnable.class)
                || type.isAnnotationPresent(RateLimit.class);
    }

    /**
     * Checks whether the method supports rate limiting.
     *
     * @param method target method
     * @return {@code true} if supported
     */
    private boolean isSupportedMethod(Method method) {
        return method.isAnnotationPresent(RateLimit.class);
    }

    /**
     * Resolves effective {@link RateLimit} annotation.
     *
     * <p>Method annotation has priority over class annotation.</p>
     *
     * @param method      invoked method
     * @param targetClass target class
     * @return resolved annotation
     */
    protected RateLimit resolveAnnotation(Method method, Class<?> targetClass) {
        RateLimit annotation = method.getAnnotation(RateLimit.class);

        if (annotation != null) {
            return annotation;
        }

        return targetClass.getAnnotation(RateLimit.class);
    }

    /**
     * Builds bucket key for current invocation.
     *
     * @param config    resolved rate-limit config
     * @param target    invocation target
     * @param method    invoked method
     * @param arguments invocation arguments
     * @return bucket key
     */
    protected BucketKey keyOf(RateLimitConfiguration config, Object target, Method method, Object[] arguments) {
        return switch (config.scope()) {
            case METHOD -> new BucketKey("M", Reflections.getMethodSignature(method), config.name());
            case CLASS  -> new BucketKey("C", target.getClass().getName(), config.name());
            case TARGET -> new BucketKey("T", System.identityHashCode(target), config.name());
            case CUSTOM -> new BucketKey("X", keyResolver.resolve(target, method, arguments), config.name());
        };
    }

    /**
     * Converts annotation to immutable runtime configuration.
     *
     * @param annotation rate-limit annotation
     * @return resolved configuration
     */
    protected RateLimitConfiguration toConfiguration(RateLimit annotation) {
        ChronoUnit      chronoUnit  = annotation.per();
        long            amount      = Math.abs(annotation.amount());
        long            periodNanos = TimeHelper.toNanos(chronoUnit, amount);
        String          name        = annotation.name() == null ? "NO_NAME" : annotation.name();
        RateLimit.Scope scope       = annotation.scope() == null ? RateLimit.Scope.METHOD : annotation.scope();
        return new RateLimitConfiguration(annotation.max(), chronoUnit, amount, periodNanos, name, scope);
    }

    /**
     * Resolved runtime configuration for a rate limit.
     *
     * @param max         maximum allowed calls
     * @param per         time unit
     * @param amount      unit amount
     * @param periodNanos period in nanoseconds
     * @param name        logical limit name
     * @param scope       bucket scope
     */
    public record RateLimitConfiguration(
            long max, ChronoUnit per, long amount, long periodNanos, String name, RateLimit.Scope scope) {
    }

    /**
     * Key of a single rate-limit bucket.
     *
     * @param scope bucket scope marker
     * @param id    bucket identity
     * @param name  logical limit name
     */
    public record BucketKey(String scope, Object id, String name) {
        @Override
        public String toString() {
            return "%s:%s:%s".formatted(scope, id, name);
        }
    }
}