package org.jmouse.web.security.firewall.policy;

import org.jmouse.core.net.CIDR;
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

public final class RequestLimitPolicy implements FirewallPolicy {

    private final RateLimitProperties      properties;
    private final List<CIDR>               trustedProxies;
    private final Map<String, TokenBucket> windows = new ConcurrentHashMap<>();

    public RequestLimitPolicy(RateLimitProperties properties, List<CIDR> trustedProxies) {
        this.properties = properties;
        this.trustedProxies = trustedProxies;
    }

    @Override
    public Decision apply(EvaluationInput evaluationInput) {
        InetAddress clientIp = null;

        if (evaluationInput.requestContext().request() instanceof WebRequest webRequest) {
            clientIp = webRequest.getClientIp();
        }

        if (clientIp != null /*&& isTrustedProxy(clientIp)*/) {
            int         refillRate  = properties.refillRate();
            int         burst       = properties.burst();
            TokenBucket tokenBucket = windows.computeIfAbsent(
                    clientIp.getHostAddress(), k -> new TokenBucket(refillRate, burst));
            if (!tokenBucket.tryAcquire()) {
                return Decision.challenge(429, "TOO MANY REQUESTS");
            }
        }

        return Decision.allow();
    }

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

