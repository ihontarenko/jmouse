package svit.proxy;

import svit.reflection.Reflections;

import java.lang.reflect.Method;
import java.util.List;

public class MethodInvocationChain implements MethodInvocation {

    protected final List<MethodInterceptor> interceptors;
    protected final Object                  target;
    protected final Method                  method;
    protected final Object[]                arguments;
    protected final Object                  proxy;
    protected final ProxyConfig             proxyConfig;
    protected       int                     currentIndex = -1;

    public MethodInvocationChain(Object proxy, Object target, Method method, Object[] arguments,
                                 List<MethodInterceptor> interceptors, ProxyConfig proxyConfig) {
        this.interceptors = interceptors;
        this.proxyConfig = proxyConfig;
        this.proxy = proxy;
        this.target = target;
        this.method = method;
        this.arguments = arguments;
    }

    @Override
    public Object proceed() throws Throwable {
        MethodInterceptor interceptor;

        // shift and execute next interceptor in the chain
        if (interceptors.size() > ++currentIndex) {
            interceptor = interceptors.get(currentIndex);

            return interceptor.invoke(this);
        }

        // invoke real method from target object in the end of chain
        return Reflections.invokeMethod(target, method, arguments);
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
    public Object getTarget() {
        return target;
    }

    @Override
    public int getOrdinal() {
        return currentIndex;
    }

    @Override
    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    @Override
    public Object getProxy() {
        return proxy;
    }
}
