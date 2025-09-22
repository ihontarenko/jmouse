package org.jmouse.context.proxy.api;

import java.lang.reflect.Method;

public interface ProxyDispatcher {

    Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable;

}
