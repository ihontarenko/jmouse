package org.jmouse.core.proxy2.aop;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.proxy2.QuadFunction;

import java.lang.reflect.Method;

public final class RuntimePointcut implements Pointcut {

    private final Matcher<Method>  matcher;
    private final RuntimePredicate predicate;

    public RuntimePointcut(Matcher<Method> matcher, RuntimePredicate predicate) {
        this.matcher = matcher;
        this.predicate = predicate;
    }

    @Override
    public Matcher<Method> methodMatcher() {
        return matcher;
    }

    @Override
    public Matcher<Class<?>> classMatcher() {
        return null;
    }

    public boolean runtimeAccept(Object proxy, Object target, Method method, Object[] arguments) {
        return predicate.apply(proxy, target, method, arguments);
    }

    @FunctionalInterface
    public interface RuntimePredicate extends QuadFunction<Object, Object, Method, Object[], Boolean> {
    }

}
