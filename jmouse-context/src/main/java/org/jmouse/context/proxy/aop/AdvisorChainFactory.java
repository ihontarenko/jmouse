package org.jmouse.context.proxy.aop;

import org.jmouse.context.proxy.api.MethodInterceptor;
import org.jmouse.core.Streamable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AdvisorChainFactory {

    private final ProxyAdvisorRegistry  registry;
    private final Map<Method, Compiled> cache = new ConcurrentHashMap<>();

    public AdvisorChainFactory(ProxyAdvisorRegistry registry) {
        this.registry = registry;
    }

    public Compiled chainFor(Class<?> type, Method method) {
        return cache.computeIfAbsent(method, m -> compile(type, method));
    }

    private Compiled compile(Class<?> type, Method method) {
        List<Advisor> matched = new ArrayList<>();

        for (Advisor advisor : registry.advisors()) {
            Pointcut pointcut = advisor.pointcut();
            if (pointcut.classMatches(type) && pointcut.methodMatches(method)) {
                matched.add(advisor);
            }
        }

        List<Pointcut>          pointcuts    = new ArrayList<>();
        List<MethodInterceptor> interceptors = Streamable.of(matched).map(Advisor::interceptor).toList();

        if (!matched.isEmpty()) {
            for (Advisor advisor : matched) {
                pointcuts.add(advisor.pointcut());
            }
        }

        return new Compiled(interceptors, pointcuts);
    }

    /**
     * Pair of interceptors and runtime pointcuts to be evaluated per invocation.
     */
    public record Compiled(List<MethodInterceptor> interceptors, List<Pointcut> pointcuts) {
    }

}
