package org.jmouse.security.pipeline;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;
import org.jmouse.security.Decision;
import org.jmouse.security.Envelope;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 🛡️ Security pipeline facade over a {@link Chain}.
 *
 * <p>Provides a type-safe wrapper that treats the pipeline itself as the
 * context type {@code C}, enabling fluent composition and specialization.</p>
 *
 * @param <C> self type that extends {@code Pipeline<C>}
 */
public abstract class Pipeline<C extends Pipeline<C, L>, L extends Link<C, Envelope, Decision>>
        implements Chain<C, Envelope, Decision> {

    private final Chain<C, Envelope, Decision> delegate;

    /**
     * 🔧 Create a pipeline over an existing chain.
     *
     * @param delegate underlying chain
     */
    protected Pipeline(Chain<C, Envelope, Decision> delegate) {
        this.delegate = delegate;
    }

    /**
     * 🧱 Compile a pipeline from links.
     *
     * <p>Builds a {@link Chain} from {@code links}, applies {@code fallback},
     * optionally reverses the order, and wraps it via {@code factory} into a concrete pipeline.</p>
     *
     * @param links     ordered links to assemble
     * @param fallback  default decision producer when chain continues
     * @param reversed  if {@code true}, links are applied in reverse order
     * @param factory   wrapper: chain → concrete pipeline instance
     * @param <C>       self type that extends {@code Pipeline<C>}
     * @return compiled pipeline
     */
    public static <C extends Pipeline<C, L>, L extends Link<C, Envelope, Decision>> Pipeline<C, L> compile(
            List<L> links,
            BiFunction<C, Envelope, Decision> fallback,
            boolean reversed,
            Function<Chain<C, Envelope, Decision>, C> factory
    ) {
        Chain.Builder<C, Envelope, Decision> builder = Chain.builder();
        builder.addAll(links).withFallback(fallback).reversed(reversed);
        return factory.apply(builder.toChain());
    }

    /**
     * ▶️ Delegate chain step.
     *
     * @param context pipeline instance as context
     * @param input   envelope to evaluate
     * @return outcome (continue or done with decision)
     */
    @Override
    public Outcome<Decision> proceed(C context, Envelope input) {
        return delegate.proceed(context, input);
    }

    @SuppressWarnings("unchecked")
    public final Outcome<Decision> proceed(Envelope envelope) {
        return proceed((C) this, envelope);
    }

    /**
     * 🎯 Run the pipeline to a concrete {@link Decision}.
     *
     * @param context  pipeline instance as context
     * @param envelope input envelope
     * @return final decision
     */
    public Decision run(C context, Envelope envelope) {
        return delegate.run(context, envelope);
    }

    /**
     * ⛓️ Compose with another pipeline.
     *
     * <p>If this pipeline continues, delegates to {@code after}.</p>
     *
     * @param after next pipeline
     * @return composed pipeline
     */
    public Pipeline<C, L> then(Pipeline<C, L> after) {
        return wrap(this.delegate.then(after.delegate));
    }

    /**
     * 🛟 Provide a fallback decision when not handled.
     *
     * @param tail fallback producer (context, envelope) → decision
     * @return pipeline with fallback
     */
    public Pipeline<C, L> withFallback(BiFunction<C, Envelope, Decision> tail) {
        return wrap(this.delegate.withFallback(tail));
    }

    /**
     * 🔍 Expose the underlying chain.
     *
     * @return inner chain
     */
    public Chain<C, Envelope, Decision> asChain() {
        return delegate;
    }

    /**
     * 🌀 Rewrap a chain into the concrete pipeline type.
     *
     * <p>Implemented by subclasses to preserve {@code C} during composition.</p>
     *
     * @param chain chain to wrap
     * @return concrete pipeline instance
     */
    protected abstract C wrap(Chain<C, Envelope, Decision> chain);

}
