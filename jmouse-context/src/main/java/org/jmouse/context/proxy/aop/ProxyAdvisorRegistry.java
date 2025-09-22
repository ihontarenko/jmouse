package org.jmouse.context.proxy.aop;

import org.jmouse.context.proxy.api.MethodInterceptor;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.el.ExpressionLanguage;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ProxyAdvisorRegistry {

    public final static Matcher<Method>   ANY_METHOD = Matcher.constant(true);
    public final static Matcher<Class<?>> ANY_TYPE   = Matcher.constant(true);

    private final ExpressionLanguage    expressionLanguage;
    private final Map<String, Pointcut> namedPointcuts = new ConcurrentHashMap<>();
    private final List<Advisor>         advisors       = new ArrayList<>();

    public ProxyAdvisorRegistry(ExpressionLanguage expressionLanguage) {
        this.expressionLanguage = expressionLanguage;
    }

    private static MethodInterceptor instantiate(Class<?> annotatedClass) {
        return (MethodInterceptor) Reflections.instantiate(Reflections.findFirstConstructor(annotatedClass));
    }

    public void registerPointcut(String name, Pointcut proxyPointcut) {
        namedPointcuts.put(name, proxyPointcut);
    }

    public Optional<Pointcut> findPointcut(String name) {
        return Optional.ofNullable(namedPointcuts.get(name));
    }

    public List<Advisor> advisors() {
        return List.copyOf(advisors);
    }

    public void scan(Class<?>... baseTypes) {
        for (Class<?> annotatedClass : ClassFinder.findAnnotatedClasses(ProxyPointcut.class, baseTypes)) {
            ProxyPointcut  annotation = annotatedClass.getAnnotation(ProxyPointcut.class);
            String         expression = annotation.value();
            RuntimeMatcher accept     = RuntimeMatcher.any();

            if (expression != null && !expression.isBlank()) {
                accept = new ExpressionRuntimeMatcher(expressionLanguage, expression);
            }

            if (!annotation.name().isEmpty()) {
                Pointcut pointcut = new SimplePointcut(ANY_TYPE, ANY_METHOD, accept);
                registerPointcut(annotation.name(), pointcut);
            }
        }

        for (Class<?> type : ClassFinder.findAnnotatedClasses(ProxyAdvice.class, baseTypes)) {
            ProxyAdvice       annotation   = type.getAnnotation(ProxyAdvice.class);
            MethodInterceptor interceptor  = instantiate(type);
            String            pointcutName = annotation.pointcut();

            Pointcut pointcut = null;
            if (!pointcutName.isBlank()) {
                pointcut = findPointcut(pointcutName).orElseThrow(() -> new IllegalStateException(
                        "UNKNOWN POINTCUT: %s".formatted(pointcutName)));
            }

            advisors.add(new Advisor(pointcut, interceptor, annotation.order()));
        }

        advisors.sort(Comparator.comparingInt(Advisor::order));
    }
}
