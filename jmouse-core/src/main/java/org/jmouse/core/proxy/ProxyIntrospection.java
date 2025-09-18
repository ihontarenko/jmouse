package org.jmouse.core.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public interface ProxyIntrospection {

    ProxyContext getProxyContext();

    static ProxyContext tryExtractContext(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof ProxyIntrospection introspection) {
            return introspection.getProxyContext();
        }

        if (Proxy.isProxyClass(object.getClass())) {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(object);
            if (invocationHandler instanceof ProxyIntrospection introspection) {
                return introspection.getProxyContext();
            }
        }

        return null;
    }

}
