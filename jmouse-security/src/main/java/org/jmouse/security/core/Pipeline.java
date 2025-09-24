package org.jmouse.security.core;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;

import java.util.List;
import java.util.function.BiFunction;
/**
 * 🔁 Universal, reusable pipeline built on top of Chain/Link.
 *
 * @param <C> shared context type (use {@code Void} if you don't need it)
 * @param <E> envelope (input) type
 */
public class Pipeline<C extends Pipeline<C>> implements Chain<C, Envelope, Decision> {

    private final Chain<C, Envelope, Decision> delegate;

    protected Pipeline(Chain<C, Envelope, Decision> delegate) {
        this.delegate = delegate;
    }

    public static <C extends Pipeline<C>> Pipeline<C> compile(
            List<? extends Link<C, Envelope, Decision>> links, BiFunction<C, Envelope, Decision> fallback, boolean reversed) {
        return new Pipeline<>(Chain.of(links, reversed).withFallback(fallback));
    }

    @Override
    public Outcome<Decision> proceed(C context, Envelope input) {
        return delegate.proceed(context, input);
    }

    public Decision run(C context, Envelope envelope) {
        return delegate.run(context, envelope);
    }

    public Pipeline<C> then(Pipeline<C> after) {
        return new Pipeline<>(this.delegate.then(after.delegate));
    }

    public Pipeline<C> withFallback(BiFunction<C, Envelope, Decision> tail) {
        return new Pipeline<>(this.delegate.withFallback(tail));
    }

    public Chain<C, Envelope, Decision> asChain() {
        return delegate;
    }

}
