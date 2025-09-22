package org.jmouse.context.proxy.aop;

import java.lang.reflect.Method;

public interface RuntimeMatcher {

    static RuntimeMatcher any() {
        return (proxy, target, method, arguments) -> true;
    }

    boolean runtimeMatches(Object proxy, Object target, Method method, Object[] arguments);

}
