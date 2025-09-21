package org.jmouse.core.proxy2.api;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 * Assembles interceptors into a linear pipeline with a terminal "invoke target".
 */
public final class InvocationPipeline {

    public static MethodInterceptor assemble(List<MethodInterceptor> interceptors, Terminal terminal) {
        return new Chained(interceptors, terminal);
    }

    public interface Terminal {
        Object call(MethodInvocation invocation) throws Throwable;
    }

    public record Chained(List<MethodInterceptor> list, Terminal terminal) implements MethodInterceptor {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            return new Cursor(invocation, list.iterator(), terminal).proceed();
        }

        private static final class Cursor implements MethodInvocation {

            private final MethodInvocation            base;
            private final Iterator<MethodInterceptor> iterator;
            private final Terminal                    terminal;

            Cursor(MethodInvocation base, Iterator<MethodInterceptor> iterator, Terminal terminal) {
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
                return 0;
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
                    return returnValue;
                }
                return terminal.call(base);
            }
        }
    }
}