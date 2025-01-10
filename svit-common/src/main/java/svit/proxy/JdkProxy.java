package svit.proxy;

import svit.reflection.Reflections;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static java.lang.reflect.Proxy.*;

public class JdkProxy implements InvocationHandler, Proxy {

    private final ProxyConfig proxyConfig;

    public JdkProxy(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
        Object   returnValue;
        Class<?> returnClass = method.getReturnType();
        Object   target      = proxyConfig.getTarget();

        try {
            if (Reflections.isEqualsMethod(method) && !proxyConfig.hasEquals()) {
                // if target does not have own 'equals' method
                return this.equals(arguments[0]);
            } else if (Reflections.isHashCodeMethod(method) && !proxyConfig.hasHashCode()) {
                // if target does not have own 'hashCode' method
                return this.hashCode();
            }

            List<MethodInterceptor> interceptors = proxyConfig.getInterceptors();
            MethodInvocation        invocation   = new MethodInvocationChain(
                    proxy, target, method, arguments, interceptors, proxyConfig);

            returnValue = invocation.proceed();
        } catch (Throwable throwable) {
            throw new ProxyInvocationException(throwable.getMessage(), throwable);
        }

        if (returnValue != null && returnValue == target && returnClass != Object.class && returnClass.isInstance(
                proxy)) {
            returnValue = proxy;
        } else if (returnValue == null && returnClass.isPrimitive() && returnClass != void.class) {
            throw new ProxyInvocationException(
                    "Method '%s' returned null, but a primitive '%s' value was expected."
                            .formatted(method, returnClass));
        }

        return returnValue;
    }

    @Override
    public Object getProxy(ClassLoader classLoader) {
        return newProxyInstance(classLoader, proxyConfig.getInterfaces().toArray(Class[]::new), this);
    }

    @Override
    public ProxyEngine getProxyEngine() {
        return ProxyEngine.JDK;
    }

    @Override
    public int hashCode() {
        return JdkProxy.class.hashCode() * 13 + proxyConfig.getTarget().hashCode();
    }

    @Override
    public boolean equals(Object that) {
        if (that == this) {
            return true;
        } else if (that == null) {
            return false;
        }

        JdkProxy proxy;

        if (that instanceof JdkProxy jdkProxy) {
            proxy = jdkProxy;
        } else if (isProxyClass(that.getClass())) {
            InvocationHandler invocationHandler = getInvocationHandler(that);
            if (!(invocationHandler instanceof JdkProxy jdkProxy)) {
                return false;
            }
            proxy = jdkProxy;
        } else {
            return false;
        }

        Class<?>[] interfacesA = proxy.proxyConfig.getInterfaces().toArray(Class[]::new);
        Class<?>[] interfacesB = proxyConfig.getInterfaces().toArray(Class[]::new);

        return Arrays.equals(interfacesA, interfacesB)
                && proxy.proxyConfig.getTarget().equals(proxyConfig.getTarget());
    }

    @Override
    public String toString() {
        return "JDK PROXY [%s] [%s]".formatted(hashCode(), super.toString());
    }
}
