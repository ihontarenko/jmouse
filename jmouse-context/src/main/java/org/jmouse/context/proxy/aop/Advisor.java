package org.jmouse.context.proxy.aop;

import org.jmouse.context.proxy.api.MethodInterceptor;

public record Advisor(Pointcut pointcut, MethodInterceptor interceptor, int order) { }