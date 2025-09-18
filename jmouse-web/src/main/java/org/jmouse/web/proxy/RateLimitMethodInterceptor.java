package org.jmouse.web.proxy;

import org.jmouse.core.throttle.*;
import org.jmouse.core.proxy.annotation.ProxyMethodInterceptor;

@ProxyMethodInterceptor({RateLimited.class})
public final class RateLimitMethodInterceptor extends AbstractRateLimitMethodInterceptor { }
