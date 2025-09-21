package org.jmouse.core.proxy2.api;

public interface ProxyEngine {

    boolean supports(ProxyDefinition<?> definition);

    <T> T createProxy(ProxyDefinition<T> definition);

}
