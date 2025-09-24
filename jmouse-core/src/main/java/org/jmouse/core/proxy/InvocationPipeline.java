package org.jmouse.core.proxy;

import org.jmouse.core.chain.Chain;

import java.util.List;

/**
 * 🧵 Builds an interceptor-based invocation pipeline.
 *
 * <p>Turns a list of {@link MethodInterceptor} into a composed {@link Chain}
 * and exposes it as a single {@link MethodInterceptor}.</p>
 */
public final class InvocationPipeline {

    /**
     * 🛠️ Assemble a single {@link MethodInterceptor} from a list.
     *
     * <p>Internally composes interceptors into a {@link Chain} and delegates to
     * {@link #assembleChain(List, Terminal)}. Any {@link Bubble} thrown inside
     * the chain is unwrapped and its cause rethrown.</p>
     *
     * @param interceptors ordered interceptors (outermost first)
     * @param terminal     terminal operation to invoke the real target
     * @return composite interceptor
     */
    public static MethodInterceptor assemble(List<? extends MethodInterceptor> interceptors, Terminal terminal) {
        return new MethodInterceptor() {
            @Override
            public Object invoke(MethodInvocation invocation) throws Throwable {
                try {
                    return assembleChain(interceptors, terminal)
                            .run(InvocationContext.forInvocation(invocation), invocation);
                } catch (Bubble bubble) {
                    throw bubble.getCause();
                }
            }
        };
    }

    /**
     * 🔗 Assemble interceptors into a {@link Chain}.
     *
     * <p>Each interceptor is wrapped as a {@code Link}. The fallback invokes
     * the {@link Terminal}. Any exception from the terminal is wrapped into
     * a {@link Bubble} to cross the chain boundary.</p>
     *
     * @param interceptors ordered interceptors (outermost first)
     * @param terminal     terminal operation for the pipeline
     * @return executable chain of interceptors
     */
    public static Chain<InvocationContext, MethodInvocation, Object> assembleChain(
            List<? extends MethodInterceptor> interceptors, Terminal terminal) {

        Chain.Builder<InvocationContext, MethodInvocation, Object> builder = Chain.builder();

        for (int i = 0; i < interceptors.size(); i++) {
            builder.add(new InterceptorLink(interceptors.get(i), i));
        }

        builder.withFallback((context, invocation) -> {
            try {
                return terminal.call(invocation);
            } catch (Throwable t) {
                throw new Bubble(t);
            }
        });

        return builder.toChain();
    }

    /**
     * 🎯 Terminal operation for a pipeline (the “real call”).
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

}
