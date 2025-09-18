package org.jmouse.core.throttle;

/**
 * 🚦 Marker interface for beans/components subject to rate limiting.
 *
 * <p>Presence of this interface can be used by the container or proxy
 * infrastructure to auto-apply {@link RateLimit} interception logic.</p>
 *
 * <ul>
 *   <li>🏷️ Does not declare any methods (pure marker).</li>
 *   <li>⚙️ Useful for conditional bean scanning or AOP pointcuts.</li>
 *   <li>🔗 Often combined with {@link AbstractRateLimitMethodInterceptor} implementations.</li>
 * </ul>
 */
public interface RateLimited {
}
