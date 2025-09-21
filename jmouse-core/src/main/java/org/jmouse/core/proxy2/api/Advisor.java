package org.jmouse.core.proxy2.api;

public record Advisor(Pointcut pointcut, MethodInterceptor interceptor) { }