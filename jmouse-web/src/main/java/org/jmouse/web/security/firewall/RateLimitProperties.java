package org.jmouse.web.security.firewall;

import org.jmouse.context.BeanProperties;
import org.jmouse.core.binding.BindDefault;

@BeanProperties("jmouse.web.servlet.firewall.token-bucket")
public record RateLimitProperties(@BindDefault("2") int refillRate, @BindDefault("4") int burst) {
    public RateLimitProperties {
        if (refillRate <= 0 || burst <= 0) {
            throw new IllegalArgumentException("Rate limit refillRate and burst must be positive");
        }
    }
}
