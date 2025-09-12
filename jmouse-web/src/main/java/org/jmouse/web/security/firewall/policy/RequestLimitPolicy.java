package org.jmouse.web.security.firewall.policy;

import org.jmouse.core.net.CIDR;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.WebRequest;
import org.jmouse.web.security.TokenBucket;
import org.jmouse.web.security.firewall.Decision;
import org.jmouse.web.security.firewall.EvaluationInput;
import org.jmouse.web.security.firewall.FirewallPolicy;
import org.jmouse.web.security.firewall.RateLimitProperties;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🚦 Firewall policy that enforces request <b>rate limiting</b>.
 *
 * <p>This policy applies a {@link TokenBucket} algorithm to limit the number of
 * requests per client IP within a defined time window. It helps prevent abuse,
 * brute-force attacks, and denial-of-service scenarios by throttling excessive traffic.</p>
 *
 * <h3>Key features</h3>
 * <ul>
 *   <li>Each client IP is tracked using its own {@link TokenBucket}.</li>
 *   <li>Rate limits are defined by {@link RateLimitProperties}:
 *     <ul>
 *       <li>{@code refillRate} → requests per second</li>
 *       <li>{@code burst} → maximum burst capacity</li>
 *     </ul>
 *   </li>
 *   <li>Trusted proxies can optionally bypass rate limiting via {@link CIDR} ranges.</li>
 *   <li>If the client exceeds the limit → request is blocked with {@code 429 TOO MANY REQUESTS}.</li>
 * </ul>
 *
 * <h3>Algorithm</h3>
 * <p>Uses a token bucket strategy:</p>
 * <pre>
 * - Tokens are refilled over time at {@code refillRate}.
 * - Each request consumes one token.
 * - If no tokens remain, the request is rejected.
 * </pre>
 *
 * <h3>Example configuration</h3>
 * <pre>
 * jmouse.web.servlet.firewall.rateLimit.refillRate = 5
 * jmouse.web.servlet.firewall.rateLimit.burst      = 10
 * jmouse.web.servlet.firewall.trustedProxy[0]      = 10.0.0.0/8
 * jmouse.web.servlet.firewall.trustedProxy[1]      = 192.168.0.0/16
 * </pre>
 *
 * @see FirewallPolicy
 * @see TokenBucket
 * @see RateLimitProperties
 */
public final class RequestLimitPolicy implements FirewallPolicy {

    /** Configured rate limit properties (refill rate and burst size). */
    private final RateLimitProperties properties;

    /** List of CIDR ranges representing trusted proxies. */
    private final List<CIDR> trustedProxies;

    /** Per-client IP token buckets. */
    private final Map<String, TokenBucket> windows = new ConcurrentHashMap<>();

    /**
     * Creates a new request rate limiting policy.
     *
     * @param properties    rate limiting parameters
     * @param trustedProxies optional list of trusted proxy ranges
     */
    public RequestLimitPolicy(RateLimitProperties properties, List<CIDR> trustedProxies) {
        this.properties = properties;
        this.trustedProxies = trustedProxies;
    }

    /**
     * Applies rate limiting to the given request.
     *
     * @param evaluationInput encapsulated request and evaluation context
     * @return {@link Decision#allow()} if the request is within the allowed rate,
     *         {@link Decision#challenge(int, String)} with 429 if the limit is exceeded
     */
    @Override
    public Decision apply(EvaluationInput evaluationInput) {
        InetAddress clientIp = null;

        if (evaluationInput.requestContext().request() instanceof WebRequest webRequest) {
            clientIp = webRequest.getClientIp();
        }

        if (clientIp != null /* && isTrustedProxy(clientIp) */) {
            int refillRate = properties.refillRate();
            int burst = properties.burst();

            TokenBucket tokenBucket = windows.computeIfAbsent(
                    clientIp.getHostAddress(),
                    k -> new TokenBucket(refillRate, burst)
            );

            if (!tokenBucket.tryAcquire()) {
                return Decision.challenge(HttpStatus.TOO_MANY_REQUESTS, "TOO MANY REQUESTS");
            }
        }

        return Decision.allow();
    }

    /**
     * Checks whether the given IP belongs to a trusted proxy.
     *
     * @param clientIp client IP to check
     * @return {@code true} if trusted, {@code false} otherwise
     */
    private boolean isTrustedProxy(InetAddress clientIp) {
        boolean trusted = trustedProxies.isEmpty();

        if (!trusted) {
            for (CIDR trustedProxy : trustedProxies) {
                if (trustedProxy.contains(clientIp)) {
                    trusted = true;
                    break;
                }
            }
        }

        return trusted;
    }
}
