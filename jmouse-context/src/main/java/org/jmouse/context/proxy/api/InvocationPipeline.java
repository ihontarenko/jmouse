package org.jmouse.context.proxy.api;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 * Assembles interceptors into a linear pipeline with a terminal "invoke target".
 */
public final class InvocationPipeline {

    public static MethodInterceptor assemble(List<? extends MethodInterceptor> interceptors, Terminal terminal) {
        return new Chained(interceptors, terminal);
    }

    public interface Terminal {
        Object call(MethodInvocation invocation) throws Throwable;
    }

    public record Chained(List<? extends MethodInterceptor> list, Terminal terminal) implements MethodInterceptor {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            return new Cursor(invocation, list.iterator(), terminal).proceed();
        }

        private static final class Cursor implements MethodInvocation {

            private final MethodInvocation                      base;
            private final Iterator<? extends MethodInterceptor> iterator;
            private final Terminal                              terminal;
            private int ordinal = 0;

            Cursor(MethodInvocation base, Iterator<? extends MethodInterceptor> iterator, Terminal terminal) {
                this.base = base;
                this.iterator = iterator;
                this.terminal = terminal;
            }

            @Override
            public Object getProxy() {
                return base.getProxy();
            }

            @Override
            public Object getTarget() {
                return base.getTarget();
            }

            @Override
            public int getOrdinal() {
                return ordinal;
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
            public Object proceed() throws Throwable {
                if (iterator.hasNext()) {
                    MethodInterceptor interceptor = iterator.next();
                    interceptor.before(null, base.getMethod(), base.getArguments());
                    Object returnValue = interceptor.invoke(this);
                    interceptor.after(null, base.getMethod(), base.getArguments(), returnValue);
                    ordinal++;
                    return returnValue;
                }
                return terminal.call(base);
            }
        }
    }
}