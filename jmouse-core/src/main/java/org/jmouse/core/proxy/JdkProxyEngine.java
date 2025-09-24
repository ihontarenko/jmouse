package org.jmouse.core.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class JdkProxyEngine implements ProxyEngine {

    @Override
    public boolean supports(ProxyDefinition<?> definition) {
        return definition.targetClass().isInterface() || !definition.extraInterfaces().isEmpty();
    }

    /**
     * 🏷️ Name of this proxy engine (e.g. "JDK", "ByteBuddy").
     *
     * @return unique engine name
     */
    @Override
    public String name() {
        return "JDK";
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

        InvocationDispatcher dispatcher = new ProxyDispatcher(this, definition);
        InvocationHandler    invocation = new JdkInvocationHandler(dispatcher, definition);

        Object proxy = Proxy.newProxyInstance(
                definition.classLoader(), interfaces.toArray(Class[]::new), invocation);

        return (T) proxy;
    }

    /**
     * Marker when no user-facing interfaces exist.
     */
    public interface Marker {
    }
}
