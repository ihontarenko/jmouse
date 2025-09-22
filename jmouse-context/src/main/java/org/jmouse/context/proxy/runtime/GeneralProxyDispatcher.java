package org.jmouse.context.proxy.runtime;

import org.jmouse.context.proxy.api.*;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.MethodMatchers;

import java.lang.reflect.Method;

/**
 * Default proxy dispatcher used by any engine (JDK/ByteBuddy/...).
 * Builds pipeline once, then uses it for every call.
 */
public final class GeneralProxyDispatcher implements ProxyDispatcher {

    private static final Matcher<Method> IS_OBJECT_METHOD =
            MethodMatchers.asMethod(MethodMatchers.isObjectMethod());

    private final ProxyDefinition<?> definition;
    private final MethodInterceptor  pipeline;

    public GeneralProxyDispatcher(ProxyDefinition<?> definition) {
        this.definition = definition;
        this.pipeline   = InvocationPipeline.assemble(definition.chain(), this::invokeTerminal);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
        arguments = (arguments == null) ? new Object[0] : arguments;

        if (IS_OBJECT_METHOD.matches(method)) {
            return switch (method.getName()) {
                case "toString" -> ("Proxy(" + definition.targetClass().getName() + ")");
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals"   -> proxy == arguments[0];
                default -> null;
            };
        }

        if (!definition.policy().shouldIntercept(method)) {
            return method.invoke(definition.instanceProvider().get(), arguments);
        }

        return pipeline.invoke(getMethodInvocation(proxy, method, arguments));
    }

    private MethodInvocation getMethodInvocation(Object proxy, Method method, Object[] args) {
        return new MethodInvocation() {
            @Override
            public Object getProxy() {
                return proxy;
            }

            @Override
            public Object getTarget() {
                return definition.instanceProvider().get();
            }

            @Override
            public int getOrdinal() {
                return -1;
            }

            @Override
            public Method getMethod() {
                return method;
            }

            @Override
            public Object[] getArguments() {
                return args;
            }

            @Override
            public Object proceed() throws Throwable {
                return invokeTerminal(this);
            }
        };
    }

    /** Outer terminal: real target call (or mixin override). */
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
