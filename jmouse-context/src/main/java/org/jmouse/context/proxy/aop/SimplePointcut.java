package org.jmouse.context.proxy.aop;

import org.jmouse.core.matcher.Matcher;

import java.lang.reflect.Method;

public final class SimplePointcut implements Pointcut, RuntimeMatcher {

    private final Matcher<Class<?>> classMatcher;
    private final Matcher<Method>   methodMatcher;
    private final RuntimeMatcher    runtimeAccept;

    public SimplePointcut(Matcher<Class<?>> classMatcher, Matcher<Method> methodMatcher, RuntimeMatcher runtimeAccept) {
        this.classMatcher = classMatcher;
        this.methodMatcher = methodMatcher;
        this.runtimeAccept = runtimeAccept;
    }

    @Override
    public Matcher<Class<?>> classMatcher() {
        return classMatcher;
    }

    @Override
    public Matcher<Method> methodMatcher() {
        return methodMatcher;
    }

    @Override
    public boolean runtimeMatches(Object proxy, Object target, Method method, Object[] arguments) {
        return runtimeAccept.runtimeMatches(proxy, target, method, arguments);
    }

}
