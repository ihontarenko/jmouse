package org.jmouse.core.proxy;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 * 🔗 Assembles a linear chain of {@link MethodInterceptor}s with a terminal invocation.
 *
 * <p>For each call, a single {@link InvocationContext} is created and passed to every
 * interceptor via {@link MethodInterceptor#before(InvocationContext, Method, Object[])},
 * {@link MethodInterceptor#after(InvocationContext, Method, Object[], Object)}, and
 * {@link MethodInterceptor#error(InvocationContext, Method, Object[], Throwable)}.</p>
 *
 * <h3>Lifecycle per invocation</h3>
 * <ol>
 *   <li>For each interceptor in order:
 *     <ol>
 *       <li>Call {@code before(...)} once</li>
 *       <li>Call {@code invoke(this)} (where {@code this} is the cursor {@link MethodInvocation})</li>
 *       <li>If {@code invoke} throws → call {@code error(...)}; if it returns {@code false}, rethrow</li>
 *       <li>Finally call {@code after(...)} (even if error was handled)</li>
 *     </ol>
 *   </li>
 *   <li>If the iterator is exhausted, call the {@link Terminal} (real target)</li>
 * </ol>
 *
 * <p>Thread-safety: instances are immutable; a new cursor is created per call.</p>
 */
public final class InvocationPipelineOld {

    /**
     * Builds a chain interceptor that routes invocations through the given interceptors
     * and finishes at the provided {@link Terminal}.
     *
     * @param interceptors ordered list of interceptors (outer → inner)
     * @param terminal     terminal handler (typically the real target call)
     * @return a {@link MethodInterceptor} representing the whole pipeline
     */
    public static MethodInterceptor assemble(List<? extends MethodInterceptor> interceptors, Terminal terminal) {
        return new Chained(interceptors, terminal);
    }

    /**
     * Terminal operation for a pipeline (the “real call”).
     */
    public interface Terminal {
        /**
         * Invokes the underlying target (or equivalent terminal behavior).
         *
         * @param invocation current invocation
         * @return method result
         * @throws Throwable any target exception
         */
        Object call(MethodInvocation invocation) throws Throwable;
    }

    /**
     * Concrete chain wrapper that exposes the pipeline as a single {@link MethodInterceptor}.
     *
     * @param list     ordered interceptors
     * @param terminal terminal call
     */
    public record Chained(List<? extends MethodInterceptor> list, Terminal terminal) implements MethodInterceptor {

        /**
         * Entrypoint for a single method call; creates a per-call cursor
         * and delegates to {@link Cursor#proceed()} to drive the chain.
         */
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            return new Cursor(invocation, list.iterator(), terminal).proceed();
        }

        /**
         * Cursor advances through the interceptor iterator and ultimately
         * reaches the terminal. Also implements {@link MethodInvocation}
         * to allow interceptors to call {@link #proceed()}.
         *
         * <p>One cursor is created per invocation.</p>
         */
        private static final class Cursor implements MethodInvocation {

            private final MethodInvocation                      base;
            private final Iterator<? extends MethodInterceptor> iterator;
            private final Terminal                              terminal;
            private final InvocationContext                     context;
            private       int                                   ordinal = 0;

            /**
             * @param base     original invocation (proxy/target/method/args)
             * @param iterator interceptor iterator
             * @param terminal terminal operation
             */
            Cursor(MethodInvocation base, Iterator<? extends MethodInterceptor> iterator, Terminal terminal) {
                this.base = base;
                this.iterator = iterator;
                this.terminal = terminal;
                this.context = InvocationContext.forInvocation(base);
            }

            /**
             * @return proxy instance for this call
             */
            @Override
            public Object getProxy() {
                return base.getProxy();
            }

            /**
             * @return current target instance (may be swapped upstream)
             */
            @Override
            public Object getTarget() {
                return base.getTarget();
            }

            /**
             * @return zero-based index of the interceptor currently executing
             * (incremented after {@code after(...)}).
             */
            @Override
            public int getOrdinal() {
                return ordinal;
            }

            /**
             * @return method being invoked
             */
            @Override
            public Method getMethod() {
                return base.getMethod();
            }

            /**
             * @return arguments of the call
             */
            @Override
            public Object[] getArguments() {
                return base.getArguments();
            }

            /**
             * Proceeds to the next interceptor, or invokes the terminal when exhausted.
             *
             * <p>Error handling contract:
             * if an interceptor's {@code invoke} throws, its {@code error(...)} is called.
             * When {@code error} returns {@code true}, the exception is considered handled
             * and the (possibly {@code null}) {@code result} is returned; otherwise the
             * original exception is rethrown.</p>
             *
             * @return result of the chain/terminal
             * @throws Throwable if an unhandled error occurs
             */
            @Override
            public Object proceed() throws Throwable {
                if (!iterator.hasNext()) {
                    return terminal.call(base);
                }

                MethodInterceptor interceptor = iterator.next();
                Object            result      = null;

                interceptor.before(context, base.getMethod(), base.getArguments());
                try {
                    result = interceptor.invoke(this);
                    return result;
                } catch (Throwable exception) {
                    Throwable throwable = exception;
                    boolean   handled   = false;

                    try {
                        handled = interceptor.error(context, base.getMethod(), base.getArguments(), throwable);
                    } catch (Throwable failed) {
                        throwable = failed;
                    }

                    if (!handled) {
                        throw throwable;
                    }

                    return result;
                } finally {
                    try {
                        interceptor.after(context, base.getMethod(), base.getArguments(), result);
                    } finally {
                        ordinal++;
                    }
                }
            }
        }
    }
}
