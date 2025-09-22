package org.jmouse.context.proxy.runtime;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bind.annotation.*;
import org.jmouse.context.proxy.api.*;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.proxy.ProxyIntrospection;
import org.jmouse.core.reflection.ClassMatchers;
import org.jmouse.core.reflection.Reflections;
import org.objenesis.ObjenesisHelper;

import java.lang.reflect.Method;

import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.*;

public final class ByteBuddyProxyEngine implements ProxyEngine {

    /**
     * 🛡️ Matcher excluding final classes.
     */
    public static final Matcher<Class<?>> NON_FINAL = Matcher.not(ClassMatchers.isFinal());
    /**
     * Hidden field to store ProxyDispatcher.
     */
    private static final String DISPATCHER            = "$dispatcher";
    /**
     * Hidden field to store ProxyDefinition for introspection.
     */
    private static final String DEFINITION            = "$definition";
    /**
     * InterceptableProxy entrypoint.
     */
    private static final String INTERNAL_INVOKE       = "internalInvoke";
    /**
     * ProxyIntrospection accessor name.
     */
    private static final String GET_DEFINITION_METHOD = "getProxyDefinition";

    @Override
    public boolean supports(ProxyDefinition<?> definition) {
        return NON_FINAL.matches(definition.targetClass());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T createProxy(ProxyDefinition<T> definition) {
        ProxyDispatcher dispatcher = new GeneralProxyDispatcher(definition);

        try {
            Class<?> proxyClass = new ByteBuddy()
                    .subclass(definition.targetClass())

                    // === InterceptableProxy: internalInvoke(Method,Object[]) ===
                    .implement(InterceptableProxy.class)
                    .defineField(DISPATCHER, ProxyDispatcher.class, Visibility.PRIVATE)
                    .defineMethod(INTERNAL_INVOKE, Object.class, Visibility.PUBLIC)
                    .withParameters(Method.class, Object[].class)
                    .intercept(
                            // internalInvoke(m, args) -> this.$dispatcher.invoke(this, m, args)
                            MethodCall.invoke(ProxyDispatcher.class
                                                      .getMethod("invoke", Object.class, Method.class, Object[].class))
                                    .onField(DISPATCHER)
                                    .withThis()
                                    .withArgument(0)
                                    .withArgument(1)
                    )

                    // === ProxyIntrospection: getProxyDefinition() ===
                    .implement(ProxyIntrospection.class)
                    .defineField(DEFINITION, ProxyDefinition.class, Visibility.PRIVATE)
                    .defineMethod(GET_DEFINITION_METHOD, ProxyDefinition.class, Visibility.PUBLIC)
                    .intercept(FieldAccessor.ofField(DEFINITION))

                    // === Route normal instance methods to Handler (reads $dispatcher field) ===
                    .method(isVirtual()
                                    .and(not(isFinalizer()))
                                    .and(not(isFinal()))
                                    .and(not(isStatic()))
                                    .and(not(isBridge()))
                                    .and(not(isSynthetic()))
                                    .and(not(isDeclaredBy(Object.class)))
                                    .and(not(isDeclaredBy(ProxyIntrospection.class)))
                                    .and(not(isDeclaredBy(InterceptableProxy.class)))
                                    .and(not(named(GET_DEFINITION_METHOD)))
                                    .and(not(named(INTERNAL_INVOKE))))
                    .intercept(to(new Handler(dispatcher)))
                    .make()
                    .load(definition.classLoader())
                    .getLoaded();

            Object proxy = ObjenesisHelper.newInstance(proxyClass);

            Reflections.setFieldValue(proxy, DEFINITION, definition);
            Reflections.setFieldValue(proxy, DISPATCHER, dispatcher);

            return (T) proxy;

        } catch (Exception e) {
            throw new IllegalStateException("ByteBuddy proxy creation failed: " + e.getMessage(), e);
        }
    }

    /** Small bridge that calls into ProxyDispatcher stored on the instance. */
    public static final class Handler {

        private final ProxyDispatcher dispatcher;

        public Handler(ProxyDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        @RuntimeType
        public Object intercept(@This Object proxy, @Origin Method method, @AllArguments Object[] arguments) throws Throwable {
            return dispatcher.invoke(proxy, method, arguments);
        }

    }
}
