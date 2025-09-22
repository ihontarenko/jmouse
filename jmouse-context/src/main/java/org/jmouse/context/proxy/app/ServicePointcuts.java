package org.jmouse.context.proxy.app;

import org.jmouse.context.proxy.aop.ProxyPointcut;

@ProxyPointcut(
        name = "gen",
        value = "method.name is starts('gen')"
)
public final class ServicePointcuts { }
