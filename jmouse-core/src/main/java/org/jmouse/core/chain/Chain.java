package org.jmouse.core.chain;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiFunction;

/**
 * ⛓️ Chain-of-responsibility abstraction with explicit {@link Outcome} control.
 *
 * <p>Each {@link Link} can:
 * <ul>
 *   <li>➡️ Return {@link Outcome.Continue} to delegate further</li>
 *   <li>✅ Return {@link Outcome.Done} to short-circuit with a result</li>
 * </ul>
 * </p>
 *
 * @param <C> context type (shared state)
 * @param <I> input type (payload)
 * @param <R> result type
 */
@FunctionalInterface
public interface Chain<C, I, R> {

    /**
     * 🏳️ Create an empty chain that always evaluates fallback.
     *
     * @param fallback function to produce a default result
     * @return chain instance
     */
    static <C, I, R> Chain<C, I, R> empty(BiFunction<C, I, R> fallback) {
        return (context, input) -> Outcome.done(fallback.apply(context, input));
    }

    /**
     * 🛠️ Start building a chain via {@link Builder}.
     *
     * @return new builder instance
     */
    static <C, I, R> Builder<C, I, R> builder() {
        return new Builder<>();
    }

    /**
     * 🏗️ Compose a chain from given links.
     *
     * @param links ordered list of links
     * @return composed chain
     */
    static <C, I, R> Chain<C, I, R> of(List<? extends Link<C, I, R>> links) {
        Builder<C, I, R> builder = builder();
        links.forEach(builder::add);
        return builder.build();
    }

    /**
     * ▶️ Proceed with current chain node.
     *
     * @param context shared context
     * @param input   input payload
     * @return outcome: either {@link Outcome.Continue} or {@link Outcome.Done}
     */
    Outcome<R> proceed(C context, I input);

    /**
     * 🚀 Run chain fully and unwrap final result.
     *
     * @param context shared context
     * @param input   input payload
     * @return result value from {@link Outcome.Done}
     * @throws IllegalStateException if no final result was produced
     */
    default R run(C context, I input) {
        Outcome<R> output = proceed(context, input);

        if (output instanceof Outcome.Done<R>(R value)) {
            return value;
        }

        throw new IllegalStateException("Chain terminated without a final result");
    }

    /**
     * 🧱 Builder for assembling a chain of {@link Link}s.
     */
    final class Builder<C, I, R> {

        private final List<Link<C, I, R>> links    = new ArrayList<>();
        private       BiFunction<C, I, R> fallback = (c, i) -> null;

        /**
         * ➕ Add a link to the chain.
         *
         * @param link link to append
         * @return this builder
         */
        public Builder<C, I, R> add(Link<C, I, R> link) {
            links.add(link);
            return this;
        }

        /**
         * 🛟 Provide a fallback function used if no link terminates the chain.
         *
         * @param function fallback producer
         * @return this builder
         */
        public Builder<C, I, R> withFallback(BiFunction<C, I, R> function) {
            this.fallback = function;
            return this;
        }

        /**
         * ✅ Build the chain in reverse order of insertion,
         * so that the first added link executes first.
         *
         * @return composed chain
         */
        public Chain<C, I, R> build() {
            ListIterator<? extends Link<C, I, R>> iterator = links.listIterator(links.size());
            Chain<C, I, R>                        chain    = Chain.empty(fallback);

            while (iterator.hasPrevious()) {
                Link<C, I, R>  link = iterator.previous();
                Chain<C, I, R> next = chain;

                chain = (context, input) -> {
                    Outcome<R> outcome = link.handle(context, input, next);

                    if (outcome instanceof Outcome.Continue<R>) {
                        return next.proceed(context, input);
                    }

                    return outcome;
                };
            }

            return chain;
        }
    }
}
