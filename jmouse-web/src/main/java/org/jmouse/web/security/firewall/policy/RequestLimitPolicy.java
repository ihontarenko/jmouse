package org.jmouse.web.security.firewall.policy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jmouse.core.net.CIDR;
import org.jmouse.core.throttle.RateLimiter;
import org.jmouse.http.HttpStatus;
import org.jmouse.web.http.WebRequest;
import org.jmouse.web.security.firewall.*;

/** Per-client token bucket. Uses the address normalized by the servlet container, never raw forwarding headers. */
public class RequestLimitPolicy implements FirewallPolicy {

    private final RateLimitProperties properties;
    private final Map<String, RateLimiter> windows = new ConcurrentHashMap<>();

    public RequestLimitPolicy(RateLimitProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * Compatibility constructor. Trusted forwarding is a server/adapter concern;
     * configure a trusted proxy valve before requests enter the servlet application.
     */
    public RequestLimitPolicy(RateLimitProperties properties, List<CIDR> trustedProxies) {
        this(properties);
    }

    @Override
    public Decision apply(EvaluationInput input) {
        String address = WebRequest.resolveClientAddress(input.requestContext().request()).getHostAddress();
        RateLimiter limiter = windows.computeIfAbsent(address, key ->
            RateLimiter.fixed(properties.refillRate(), properties.burst())
        );
        return limiter.tryAcquire()
            ? Decision.allow()
            : Decision.challenge(HttpStatus.TOO_MANY_REQUESTS, "Request rate limit exceeded");
    }
}
