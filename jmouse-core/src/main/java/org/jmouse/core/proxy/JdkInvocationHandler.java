package org.jmouse.core.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class JdkInvocationHandler implements InvocationHandler, ProxyIntrospection {

    private final ProxyDispatcher    dispatcher;
    private final ProxyDefinition<?> definition;

    public JdkInvocationHandler(ProxyDispatcher dispatcher, ProxyDefinition<?> definition) {
        this.dispatcher = dispatcher;
        this.definition = definition;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
        return dispatcher.invoke(proxy, method, arguments);
    }

    @Override
    public ProxyDefinition<?> getProxyDefinition() {
        return definition;
    }

}
