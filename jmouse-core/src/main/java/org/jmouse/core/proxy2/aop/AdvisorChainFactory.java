package org.jmouse.core.proxy2.aop;

import org.jmouse.core.Streamable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AdvisorChainFactory {

    private final List<Advisor>              advisors = new ArrayList<>();
    private final Map<Method, CompiledChain> cache    = new ConcurrentHashMap<>();

    public CompiledChain chainFor(Class<?> type, Method method) {
        return cache.computeIfAbsent(method, m -> compile(type, method));
    }

    private CompiledChain compile(Class<?> type, Method method) {
        List<Advisor> matched = new ArrayList<>();

        for (Advisor advisor : advisors) {
            Pointcut pointcut = advisor.pointcut();
            if (pointcut.classMatches(type) && pointcut.methodMatches(method)) {
                matched.add(advisor);
            }
        }

        List<RuntimePointcut> pointcuts = new ArrayList<>();
        List<Advice> advices = Streamable.of(matched).map(Advisor::advice).toList();

        if (!matched.isEmpty()) {
            for (Advisor advisor : matched) {
                Pointcut pointcut = advisor.pointcut();
                if (pointcut instanceof RuntimePointcut runtimePointcut) {
                    pointcuts.add(runtimePointcut);
                }
            }
        }

        return new CompiledChain(advices, pointcuts);
    }

    /** Pair of interceptors and runtime pointcuts to be evaluated per invocation. */
    public record CompiledChain(List<Advice> interceptors,
                                List<RuntimePointcut> pointcuts) { }

}
