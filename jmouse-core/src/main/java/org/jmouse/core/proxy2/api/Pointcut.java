package org.jmouse.core.proxy2.api;

import org.jmouse.core.matcher.Matcher;

import java.lang.reflect.Method;

public interface Pointcut {
    Matcher<Method> methodMatcher();
    default boolean classMatches(Class<?> type) { return true; }
}
