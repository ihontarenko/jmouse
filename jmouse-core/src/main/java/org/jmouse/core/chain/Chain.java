package org.jmouse.core.chain;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
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
    static <C, I, R> Chain<C, I, R> of(List<? extends Link<C, I, R>> links, boolean reverse) {
        Builder<C, I, R> builder = builder();
        links.forEach(builder::add);
        return builder.reversed(reverse).toChain();
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

    default R perform(C context, I input) {
        return (proceed(context, input) instanceof Outcome.Done<R>(R value)) ? value : null;
    }

    default Optional<R> tryRun(C context, I input) {
        return Optional.ofNullable(perform(context, input));
    }

    default Chain<C, I, R> withFallback(BiFunction<C, I, R> fallback) {
        Chain<C, I, R> self = this;
        return (context, input) -> {
            Outcome<R> outcome = self.proceed(context, input);
            return (outcome instanceof Outcome.Continue<R>)
                    ? Outcome.done(fallback.apply(context, input)) : outcome;
        };
    }

    default Chain<C, I, R> then(Chain<C, I, R> after) {
        Chain<C, I, R> self = this;
        return (context, input) -> {
            Outcome<R> outcome = self.proceed(context, input);
            return (outcome instanceof Outcome.Continue<R>) ? after.proceed(context, input) : outcome;
        };
    }

    static <C, I, R> Chain<C, I, R> of(Link<C, I, R> link, BiFunction<C, I, R> fallback) {
        return Chain.<C, I, R>builder().add(link).withFallback(fallback).toChain();
    }

    /**
     * 🧱 Builder for assembling a chain of {@link Link}s.
     */
    final class Builder<C, I, R> {

        private final List<Link<C, I, R>> links    = new ArrayList<>();
        private       BiFunction<C, I, R> fallback = (c, i) -> null;
        private       boolean             reversed = false;

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
         * 🔄 Configure whether the chain should be executed in reverse order.
         *
         * <p>By default, resolvers/handlers are applied in the order they were registered.
         * Setting this flag to {@code true} will invert the execution sequence.</p>
         *
         * @param reversed {@code true} to run the chain in reverse
         * @return this builder
         */
        public Builder<C, I, R> reversed(boolean reversed) {
            this.reversed = reversed;
            return this;
        }        /**
         * ⬆️ Add link to the beginning of the chain.
         *
         * @param link link to add
         * @return this builder
         */
        public Builder<C, I, R> addFirst(Link<C, I, R> link) {
            links.addFirst(link);
            return this;
        }

        /**
         * ⬇️ Add link to the end of the chain.
         *
         * @param link link to add
         * @return this builder
         */
        public Builder<C, I, R> addLast(Link<C, I, R> link) {
            links.addLast(link);
            return this;
        }

        /**
         * 📦 Add a batch of links to the chain.
         *
         * @param more list of links
         * @return this builder
         */
        public Builder<C, I, R> addAll(List<? extends Link<C, I, R>> more) {
            links.addAll(more);
            return this;
        }

        /**
         * 🌀 Wrap the chain with an outer link.
         *
         * <p>Alias for {@link #addFirst(Link)}.</p>
         *
         * @param around outer link
         * @return this builder
         */
        public Builder<C, I, R> wrap(Link<C, I, R> around) {
            return addFirst(around);
        }

        /**
         * 🛡️ Build a chain with guaranteed finalization.
         *
         * <p>Ensures that {@code finite} link is always invoked
         * after execution, even if exceptions occur.</p>
         *
         * @param finite finalization link (safe guard)
         * @return safe chain instance
         */
        public Chain<C, I, R> toSafeChain(Link<C, I, R> finite) {
            Chain<C, I, R> core = toChain();
            return (context, input) -> {
                try {
                    return core.proceed(context, input);
                } finally {
                    try {
                        finite.handle(context, input, Chain.empty((c, i) -> null));
                    } catch (Throwable ignore) {}
                }
            };
        }


        /**
         * ✅ Build the chain in reverse order of insertion,
         * so that the first added link executes first.
         *
         * @return composed chain
         */
        public Chain<C, I, R> toChain() {
            ListIterator<? extends Link<C, I, R>> iterator = links.listIterator(reversed ? 0 : links.size());
            Chain<C, I, R>                        chain    = Chain.empty(fallback);

            while (reversed ? iterator.hasNext() : iterator.hasPrevious()) {
                Link<C, I, R>  link = reversed ? iterator.next() : iterator.previous();
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
