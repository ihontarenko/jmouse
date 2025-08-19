package org.jmouse.web.security.firewall;

import org.jmouse.context.BeanProperties;

@BeanProperties("jmouse.web.servlet.firewall.token-bucket")
public record RateLimitProperties(int refillRate, int burst) {
}
