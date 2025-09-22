package org.jmouse.core.proxy.old;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.proxy.*;
import org.jmouse.core.reflection.ClassMatchers;
import org.jmouse.core.reflection.Reflections;
import org.objenesis.ObjenesisHelper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.jmouse.core.proxy.old.ProxyInvoke.invokeCore;

/**
 * 🧬 {@link ProxyEngine} implementation using ByteBuddy.
 *
 * <p>Generates subclass-based proxies for non-final concrete classes,
 * routing all virtual methods through the framework’s invocation pipeline.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>⚡ Subclass proxying (no interfaces required).</li>
 *   <li>📦 Exposes {@link ProxyContext} via {@link ProxyIntrospection}.</li>
 *   <li>🪝 Provides {@link InterceptableProxy} entrypoint
 *       ({@link #INTERNAL_INVOKE}).</li>
 *   <li>♻️ Delegates method calls into {@link ProxyInvoke#invokeCore(String, ProxyContext, Object, Method, Object[])}.</li>
 *   <li>🧠 Fallbacks for {@code equals}/{@code hashCode}/{@code toString}.</li>
 * </ul>
 *
 * <h3>Limitations</h3>
 * <ul>
 *   <li>🚫 Cannot proxy interfaces (use {@link JdkProxyEngine}).</li>
 *   <li>🚫 Cannot proxy final classes or final methods.</li>
 *   <li>🚫 Ignores bridge/synthetic methods and helper API methods.</li>
 * </ul>
 */
public class ByteBuddyProxyEngine implements ProxyEngine {

    /**
     * 🛡️ Matcher excluding final classes.
     */
    public static final Matcher<Class<?>> NON_FINAL = Matcher.not(ClassMatchers.isFinal());

    /**
     * 🔒 Internal dispatch method (must match {@link InterceptableProxy}).
     */
    public static final String INTERNAL_INVOKE = "internalInvoke";

    /**
     * 🧷 Hidden field for JDK-style {@link InvocationHandler}.
     */
    public static final String INVOCATION_HANDLER = "$jdkInvocationHandler";

    /**
     * 📦 Hidden field to store {@link ProxyContext}.
     */
    public static final String FIELD_CONTEXT = "$proxyContext";

    /**
     * 🏷️ Engine identifier.
     */
    public static final String ENGINE_NAME = "BYTE_BUDDY";

    /**
     * 🔗 Method name exposing context (must match {@link ProxyIntrospection}).
     */
    public static final String GET_PROXY_CONTEXT_METHOD = "getProxyContext";

    /**
     * 🏗️ Create a ByteBuddy-generated proxy for the given {@link ProxyContext}.
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Subclass target class + implement interfaces.</li>
     *   <li>Expose context field + accessor via {@link ProxyIntrospection}.</li>
     *   <li>Expose internal {@link InterceptableProxy} entrypoint.</li>
     *   <li>Route all eligible methods into {@link Dispatcher}.</li>
     *   <li>Instantiate proxy via {@link ObjenesisHelper} (no constructor call).</li>
     * </ol>
     *
     * @throws ProxyInvocationException if proxy generation fails
     */
    @Override
    public Object createProxy(ProxyContext context) {
        try {
            Class<?> proxyClass = new ByteBuddy()
                    .subclass(context.getTargetClass())
                    // --- internal entrypoint mimicking JDK InvocationHandler
                    .implement(InterceptableProxy.class)
                    .defineField(INVOCATION_HANDLER, InvocationHandler.class, Visibility.PRIVATE)
                    .defineMethod(INTERNAL_INVOKE, Object.class, Visibility.PUBLIC)
                    .withParameters(Method.class, Object[].class)
                    .intercept(MethodCall.invoke(
                            InvocationHandler.class.getMethod("invoke", Object.class, Method.class, Object[].class))
                                       .onField(INVOCATION_HANDLER)
                                       .withThis()
                                       .withArgument(0)
                                       .withArgument(1))
                    // --- expose ProxyContext
                    .implement(ProxyIntrospection.class)
                    .defineField(FIELD_CONTEXT, ProxyContext.class, Visibility.PRIVATE)
                    .defineMethod(GET_PROXY_CONTEXT_METHOD, ProxyContext.class, Visibility.PUBLIC)
                    .intercept(FieldAccessor.ofField(FIELD_CONTEXT))
                    // --- route regular instance methods to Dispatcher
                    .method(isVirtual()
                                    .and(not(isFinalizer()))
                                    .and(not(isFinal()))
                                    .and(not(isStatic()))
                                    .and(not(isBridge()))
                                    .and(not(isSynthetic()))
                                    // exclude helper APIs
                                    .and(not(isDeclaredBy(ProxyIntrospection.class)))
                                    .and(not(isDeclaredBy(InterceptableProxy.class)))
                                    .and(not(named(GET_PROXY_CONTEXT_METHOD)))
                                    .and(not(named(INTERNAL_INVOKE)))
                    )
                    .intercept(to(new Dispatcher(context)))
                    // --- build
                    .make()
                    .load(context.getClassLoader())
                    .getLoaded();

            Object            proxy   = ObjenesisHelper.newInstance(proxyClass);
            InvocationHandler handler = (t, m, a) -> invokeCore(ENGINE_NAME, context, t, m, a);

            Reflections.setFieldValue(proxy, FIELD_CONTEXT, context);
            Reflections.setFieldValue(proxy, INVOCATION_HANDLER, handler);

            return proxy;

        } catch (Exception e) {
            throw new ProxyInvocationException("ByteBuddy proxy creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ Supports non-interface, non-final classes without extra interfaces.
     */
    @Override
    public boolean supports(ProxyContext context) {
        Class<?> type = context.getTargetClass();
        return !type.isInterface() && context.getInterfaces().isEmpty() && NON_FINAL.matches(type);
    }

    /**
     * 🏷️ Engine name ("BYTE_BUDDY").
     */
    @Override
    public String name() {
        return ENGINE_NAME;
    }

    /**
     * 🛣️ Delegation bridge from generated proxy to interceptor chain.
     *
     * <p>All routed methods land here, which delegates to
     * {@link ProxyInvoke#invokeCore(String, ProxyContext, Object, Method, Object[])}.</p>
     */
    public static final class Dispatcher {
        private final ProxyContext context;

        /**
         * 🏗️ Bind dispatcher to a specific proxy context.
         */
        public Dispatcher(ProxyContext context) {
            this.context = context;
        }

        /**
         * 🔁 Intercept a method call and run the unified pipeline.
         */
        @RuntimeType
        public Object intercept(@This Object proxy, @Origin Method method, @AllArguments Object[] arguments) {
            return invokeCore(ENGINE_NAME, context, proxy, method, arguments);
        }

        /**
         * 🧾 Human-readable descriptor for debugging/logs.
         */
        @Override
        public String toString() {
            return "BYTE-BUDDY PROXY [%s]".formatted(context.getTargetClass());
        }
    }
}
