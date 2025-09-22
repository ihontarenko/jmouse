package org.jmouse.context.proxy.runtime;

import org.jmouse.context.proxy.api.*;

import java.lang.reflect.*;
import java.util.*;

public final class JdkProxyEngine implements ProxyEngine {

    @Override
    public boolean supports(ProxyDefinition<?> definition) {
        return definition.targetClass().isInterface() || !definition.extraInterfaces().isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T createProxy(ProxyDefinition<T> definition) {
        Set<Class<?>> interfaces = new LinkedHashSet<>();

        if (definition.targetClass().isInterface()) {
            interfaces.add(definition.targetClass());
        }

        interfaces.addAll(Arrays.asList(definition.targetClass().getInterfaces()));
        interfaces.addAll(definition.extraInterfaces());
        interfaces.addAll(definition.mixins().implementations().keySet());

        if (interfaces.isEmpty()) {
            interfaces.add(Marker.class);
        }

        ProxyDispatcher   dispatcher = new GeneralProxyDispatcher(definition);
        InvocationHandler invocation = new JdkInvocationHandler(dispatcher, definition);

        Object proxy = Proxy.newProxyInstance(
                definition.classLoader(), interfaces.toArray(Class[]::new), invocation);

        return (T) proxy;
    }

    /** Marker when no user-facing interfaces exist. */
    public interface Marker {}
}
