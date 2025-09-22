package org.jmouse.context.proxy.api;

public interface ProxyEngine {

    boolean supports(ProxyDefinition<?> definition);

    <T> T createProxy(ProxyDefinition<T> definition);

}
