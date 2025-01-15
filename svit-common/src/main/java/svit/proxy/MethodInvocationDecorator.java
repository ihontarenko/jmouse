package svit.proxy;

import java.lang.reflect.Method;
import java.util.Optional;

import static java.util.Arrays.stream;

public class MethodInvocationDecorator implements MethodInvocation {

    protected final MethodInvocation delegate;

    public MethodInvocationDecorator(MethodInvocation delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings({"unchecked"})
    public <T> Optional<T> getTypedArgument(Class<T> type) {
        return (Optional<T>) stream(delegate.getArguments())
                .filter(argument -> type.isAssignableFrom(argument.getClass())).findFirst();
    }

    public String getMethodName() {
        return getMethod().getName();
    }

    public Class<?> getMethodClass() {
        return getMethod().getDeclaringClass();
    }

    public String getMethodClassName() {
        return getMethodClass().getName();
    }

    public Class<?> getThisClass() {
        return getTarget().getClass();
    }

    @Override
    public Object[] getArguments() {
        return delegate.getArguments();
    }

    @Override
    public Method getMethod() {
        return delegate.getMethod();
    }

    @Override
    public Object getTarget() {
        return delegate.getTarget();
    }

    @Override
    public int getOrdinal() {
        return delegate.getOrdinal();
    }

    @Override
    public ProxyContext getProxyContext() {
        return delegate.getProxyContext();
    }

    @Override
    public Object getProxy() {
        return delegate.getProxy();
    }

    @Override
    public Object proceed() throws Throwable {
        return delegate.proceed();
    }

}
