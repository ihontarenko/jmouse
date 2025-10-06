package org.jmouse.core.proxy;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.MethodMatchers;
import org.jmouse.core.reflection.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 🎭 General-purpose proxy dispatcher.
 *
 * <p>Acts as the central dispatcher for proxy method calls. Used by any proxy engine
 * (JDK, ByteBuddy, etc.). Builds a reusable interceptor pipeline once, and re-applies
 * it for each subsequent invocation.</p>
 *
 * <ul>
 *   <li>Delegates calls to {@link InvocationPipeline} for AOP chain execution</li>
 *   <li>Handles {@code Object} methods (toString, hashCode, equals) directly</li>
 *   <li>Falls back to real target instance or mixin override when needed</li>
 * </ul>
 */
public final class ProxyDispatcher implements InvocationDispatcher {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProxyDispatcher.class);

    /**
     * ✅ Matcher for {@code Object} methods (toString, hashCode, equals).
     */
    private static final Matcher<Method> IS_OBJECT_METHOD = MethodMatchers.asMethod(MethodMatchers.isObjectMethod());

    private final ProxyEngine        engine;
    private final ProxyDefinition<?> definition;
    private final MethodInterceptor  pipeline;

    /**
     * Constructs a dispatcher for the given proxy definition.
     *
     * @param definition proxy definition containing chain, mixins, and policies
     */
    public ProxyDispatcher(ProxyEngine engine, ProxyDefinition<?> definition) {
        this.engine = engine;
        this.definition = definition;
        this.pipeline = InvocationPipeline.assemble(definition.chain(), this::invokeTerminal);
    }

    /**
     * Dispatches a method invocation to the proxy pipeline.
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>If method is from {@code Object}, handle directly</li>
     *   <li>If policy denies interception, invoke target instance directly</li>
     *   <li>Otherwise, pass through interceptor pipeline</li>
     * </ol>
     *
     * @param proxy     proxy instance
     * @param method    method being called
     * @param arguments arguments of the call (may be {@code null})
     * @return result of invocation
     * @throws Throwable if the target or interceptors throw
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
        arguments = (arguments == null) ? new Object[0] : arguments;

        if (IS_OBJECT_METHOD.matches(method)) {
            return switch (method.getName()) {
                case "toString" -> ("jMouseProxy_" + engine.name() + "(" + definition.targetClass().getName() + ")");
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == arguments[0];
                default -> throw new ProxyInvocationException("Unexpected object-method: " + method);
            };
        }

        if (!definition.policy().shouldIntercept(method)) {
            LOGGER.warn("Method {} is not intercepted by proxy", method.getName());
            return method.invoke(definition.instanceProvider().get(), arguments);
        }

        return pipeline.invoke(getMethodInvocation(proxy, method, arguments));
    }

    /**
     * Creates a {@link MethodInvocation} wrapper for pipeline processing.
     *
     * @param proxy  proxy instance
     * @param method invoked method
     * @param arguments   arguments
     * @return invocation wrapper
     */
    private MethodInvocation getMethodInvocation(Object proxy, Method method, Object[] arguments) {
        return new MethodInvocation() {

            private Object returnValue;
            // private Object[] arguments;

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
                return arguments;
            }

            @Override
            public Object proceed() throws Throwable {
                return invokeTerminal(this);
            }

            @Override
            public Object getProxy() {
                return proxy;
            }

            @Override
            public Object getReturnValue() {
                return returnValue;
            }

            @Override
            public void setReturnValue(Object returnValue) {
                this.returnValue = returnValue;
            }

            @Override
            public String toString() {
                return "anonymous [%s] : %s".formatted(
                        ProxyDispatcher.this.engine.name(), Reflections.getMethodName(method));
            }
        };
    }

    /**
     * 🔚 Terminal invocation: invokes the real target or mixin override.
     *
     * <p>Checks if a mixin provides an implementation for the declaring class
     * of the method. If yes, delegates to mixin. Otherwise, falls back to
     * the underlying target instance.</p>
     *
     * @param invocation method invocation wrapper
     * @return result of the real call
     * @throws Throwable if target throws
     */
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
