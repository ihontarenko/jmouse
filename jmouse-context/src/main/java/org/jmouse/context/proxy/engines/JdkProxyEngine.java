package org.jmouse.context.proxy.engines;

import org.jmouse.context.proxy.api.*;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.MethodMatchers;

import java.lang.reflect.*;
import java.util.*;

public final class JdkProxyEngine implements ProxyEngine {

    private static final Matcher<Method> IS_OBJECT_METHOD = MethodMatchers.asMethod(MethodMatchers.isObjectMethod());

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

        ClassLoader       classLoader       = definition.options().classLoader();
        InvocationHandler invocationHandler = new Dispatcher(definition);
        Object            proxy             = Proxy.newProxyInstance(
                classLoader, interfaces.toArray(Class[]::new), invocationHandler);

        return (T) proxy;
    }

    /** Marker interface used when no user interfaces are present. */
    public interface Marker {}

    public static final class Dispatcher implements InvocationHandler {
        private final ProxyDefinition<?> definition;
        private final MethodInterceptor  pipeline;

        public Dispatcher(ProxyDefinition<?> definition) {
            this.definition = definition;
            this.pipeline = InvocationPipeline.assemble(definition.chain(), this::invokeTerminal);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
            arguments = arguments == null ? new Object[0] : arguments;

            if (IS_OBJECT_METHOD.matches(method)) {
                return switch (method.getName()) {
                    case "toString" -> ("JdkProxy(" + definition.targetClass().getName() + ")");
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals"   -> proxy == arguments[0];
                    default -> null;
                };
            }

            if (!definition.policy().shouldIntercept(method)) {
                return method.invoke(definition.target().get(), arguments);
            }

            MethodInvocation invocation = getMethodInvocation(proxy, method, arguments);

            return pipeline.invoke(invocation);
        }

        private MethodInvocation getMethodInvocation(Object proxy, Method method, Object[] arguments) {
            return new MethodInvocation() {
                @Override
                public Object getProxy() {
                    return proxy;
                }

                @Override
                public Object getTarget() {
                    return definition.target().get();
                }

                @Override
                public int getOrdinal() {
                    return 0;
                }

                @Override
                public Method getMethod() {
                    return method;
                }

                @Override
                public Object[] getArguments() {
                    return arguments;
                }

                @Override
                public Object proceed() throws Throwable {
                    return invokeTerminal(this);
                }
            };
        }

        private Object invokeTerminal(MethodInvocation invocation) throws Throwable {
            Method method = invocation.getMethod();
            Object mixin  = definition.mixins().implementationFor(method.getDeclaringClass());
            Object target = invocation.getTarget();

            if (mixin != null) {
                target = mixin;
            }

            return method.invoke(target, invocation.getArguments());
        }
    }
}
