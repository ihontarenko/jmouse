package org.jmouse.core.throttle;

import org.jmouse.core.proxy.MethodInterceptor;
import org.jmouse.core.proxy.ProxyContext;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.TimeHelper;

import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🚦 Abstract interceptor enforcing {@link RateLimit} constraints.
 *
 * <p>Intercepts method calls and ensures they respect declared rate limits.
 * Each invocation attempts to acquire a token from the appropriate {@link RateLimiter}
 * bucket (derived from method/class/instance/custom scope).</p>
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>🔎 Resolve {@link RateLimit} annotation (method → class).</li>
 *   <li>⚙️ Convert to {@link RateLimitConfiguration} with period in nanoseconds.</li>
 *   <li>🗝️ Compute {@link BucketKey} based on scope.</li>
 *   <li>🪣 Fetch/create a {@link RateLimiter} bucket and attempt token acquisition.</li>
 *   <li>⛔ Throw {@link RateLimitExceededException} if bucket is exhausted.</li>
 * </ol>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>🧵 Thread-safe buckets via {@link ConcurrentHashMap}.</li>
 *   <li>⚡ Default custom scope resolver: {@link MethodArgumentsHashKeyResolver}.</li>
 *   <li>🔧 Override methods to plug in custom annotation resolution or keying.</li>
 * </ul>
 */
public abstract class AbstractRateLimitMethodInterceptor implements MethodInterceptor {

    /**
     * 🔑 Resolver for custom keys (used when {@link RateLimit.Scope#CUSTOM}).
     * Defaults to {@link MethodArgumentsHashKeyResolver}.
     */
    private final RateLimitKeyResolver keyResolver;

    /**
     * 🪣 Map of active buckets keyed by {@link BucketKey}.
     */
    private final Map<BucketKey, RateLimiter> buckets = new ConcurrentHashMap<>();

    /**
     * 🏗️ Create an interceptor with a default
     * {@link MethodArgumentsHashKeyResolver} for custom scopes.
     */
    public AbstractRateLimitMethodInterceptor() {
        this(new MethodArgumentsHashKeyResolver());
    }

    /**
     * 🏗️ Create an interceptor with a given custom key resolver.
     *
     * @param keyResolver resolver for custom scope keys
     */
    public AbstractRateLimitMethodInterceptor(RateLimitKeyResolver keyResolver) {
        this.keyResolver = keyResolver;
    }

    /**
     * ⛔ Invoked before method execution.
     *
     * <p>Checks the rate limit and either allows the call or throws
     * {@link RateLimitExceededException}.</p>
     *
     * @throws RateLimitExceededException if limit exceeded
     */
    @Override
    public void before(ProxyContext context, Method method, Object[] arguments) {
        if (isSupportedType(context.getTargetClass()) && isSupportedMethod(method)) {
            RateLimit              annotation = resolveAnnotation(method, context.getTarget().getClass());
            RateLimitConfiguration config     = toConfiguration(annotation);
            BucketKey              key        = keyOf(config, context.getTarget(), method, arguments);
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

    private boolean isSupportedType(Class<?> type) {
        return RateLimited.class.isAssignableFrom(type)
                || type.isAnnotationPresent(RateLimitEnable.class)
                || type.isAnnotationPresent(RateLimit.class);
    }

    private boolean isSupportedMethod(Method method) {
        return method.isAnnotationPresent(RateLimit.class);
    }

    /**
     * 🔎 Resolve {@link RateLimit} from method or fallback to target class.
     */
    protected RateLimit resolveAnnotation(Method method, Class<?> targetClass) {
        RateLimit annotation = method.getAnnotation(RateLimit.class);

        if (annotation != null) {
            return annotation;
        }

        return targetClass.getAnnotation(RateLimit.class);
    }

    /**
     * 🗝️ Build a {@link BucketKey} for the given configuration and context.
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
     * ⚙️ Convert {@link RateLimit} annotation into an immutable config.
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
     * 📦 Immutable resolved configuration for a rate limit.
     */
    public record RateLimitConfiguration(
            long max, ChronoUnit per, long amount, long periodNanos, String name, RateLimit.Scope scope) {
    }

    /**
     * 🪪 Key identifying a unique bucket.
     *
     * <p>Format: {@code scope:id:name}</p>
     */
    public record BucketKey(String scope, Object id, String name) {
        @Override
        public String toString() {
            return "%s:%s:%s".formatted(scope, id, name);
        }
    }
}
