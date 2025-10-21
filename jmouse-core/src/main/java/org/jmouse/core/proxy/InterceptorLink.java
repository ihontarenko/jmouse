package org.jmouse.core.proxy;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;

import java.lang.reflect.Method;

public final class InterceptorLink implements Link<InvocationContext, MethodInvocation, Object> {

    private final MethodInterceptor interceptor;
    private final int               ordinal;

    public InterceptorLink(MethodInterceptor interceptor, int ordinal) {
        this.interceptor = interceptor;
        this.ordinal = ordinal;
    }

    @Override
    public Outcome<Object> handle(
            InvocationContext context, MethodInvocation base, Chain<InvocationContext, MethodInvocation, Object> next) {

        MethodInvocation bridge = new MethodInvocation() {

            @Override
            public Object getProxy() {
                return base.getProxy();
            }

            @Override
            public Object getTarget() {
                return base.getTarget();
            }

            @Override
            public Method getMethod() {
                return base.getMethod();
            }

            @Override
            public Object[] getArguments() {
                return base.getArguments();
            }

            @Override
            public void setArgumentsUnsafe(Object[] arguments) {
                base.setArgumentsUnsafe(arguments);
            }

            @Override
            public int getOrdinal() {
                return ordinal;
            }

            @Override
            public Object getReturnValue() {
                return base.getReturnValue();
            }

            @Override
            public void setReturnValue(Object returnValue) {
                base.setReturnValue(returnValue);
            }

            @Override
            public String toString() {
                return "anonymous [%s] : %s".formatted(InterceptorLink.this.toString(), base.toString());
            }

            @Override
            public Object proceed() throws Throwable {
                try {
                    Outcome<Object> outcome = next.proceed(context, base);
                    Object          result  = null;

                    if (outcome instanceof Outcome.Done<Object>(Object value)) {
                        result = value;
                    }

                    return result;
                } catch (Bubble b) {
                    throw b.getCause();
                }
            }
        };

        Object result = null;

        try {
            interceptor.before(context, base.getMethod(), base.getArguments());
            result = interceptor.invoke(bridge);
            bridge.setReturnValue(result);
            return Outcome.done(result);
        } catch (Throwable exception) {
            handle(exception, context, base);
            return Outcome.done(result);
        } finally {
            try {
                interceptor.after(context, base.getMethod(), base.getArguments(), result);
            } catch (Throwable throwable) {
                handle(throwable, context, base);
            }
        }
    }

    private void handle(Throwable throwable, InvocationContext context, MethodInvocation invocation) {
        boolean handled = false;

        try {
            handled = interceptor.error(context, invocation.getMethod(), invocation.getArguments(), throwable);
        } catch (Throwable failed) {
            throwable.addSuppressed(failed);
        }

        if (!handled) {
            throw new Bubble(throwable);
        }
    }

    @Override
    public String toString() {
        return "InterceptorLink[%d] : %s".formatted(ordinal, interceptor.getClass().getSimpleName());
    }
}