package org.jmouse.context.proxy.api;

public record ProxyOptions(
        boolean exposeProxy,
        boolean cacheGeneratedClasses,
        ClassLoader classLoader
) {
    public static ProxyOptions defaults() {
        return new ProxyOptions(false, true, Thread.currentThread().getContextClassLoader());
    }
}