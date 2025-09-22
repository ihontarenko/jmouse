package org.jmouse.core.proxy2.aop;

import org.jmouse.core.matcher.Matcher;

import java.lang.reflect.Method;

public interface Pointcut {

    Matcher<Method> methodMatcher();

    Matcher<Class<?>> classMatcher();

    default boolean classMatches(Class<?> type) {
        return classMatcher().matches(type);
    }

    default boolean methodMatches(Method method) {
        return methodMatcher().matches(method);
    }
}
