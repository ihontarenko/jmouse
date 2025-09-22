package org.jmouse.context.proxy.support;

import org.jmouse.context.proxy.api.ProxyDefinition;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public interface ProxyIntrospection {

    ProxyDefinition<?> getProxyDefinition();

    static ProxyDefinition<?> tryExtractDefinition(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof ProxyIntrospection introspection) {
            return introspection.getProxyDefinition();
        }

        if (Proxy.isProxyClass(object.getClass())) {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(object);
            if (invocationHandler instanceof ProxyIntrospection introspection) {
                return introspection.getProxyDefinition();
            }
        }

        return null;
    }

}
