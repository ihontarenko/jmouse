package org.jmouse.core.proxy;

abstract public class AbstractProxyFactory implements ProxyFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Object object) {
        return createProxy((ProxyDefinition<T>) ProxyDefinition.createDefault(object));
    }

}
