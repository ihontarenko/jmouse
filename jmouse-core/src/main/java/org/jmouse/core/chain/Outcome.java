package org.jmouse.core.chain;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 🎯 Result of a chain step: either continue evaluation or stop with a final value.
 *
 * <p>Used together with {@link Chain} / {@link Link} to model explicit
 * continuation or short-circuiting without relying on {@code null}.</p>
 *
 * @param <R> result type
 */
public sealed interface Outcome<R> permits Outcome.Continue, Outcome.Done {

    /**
     * ➡️ Signal to continue chain evaluation.
     *
     * @return singleton instance of {@link Continue}
     */
    @SuppressWarnings("unchecked")
    static <R> Outcome<R> next() {
        return (Continue<R>) Continue.INSTANCE;
    }

    /**
     * 🛑 Produce a final value that terminates the chain.
     *
     * @param value result to return
     * @param <R>   type of result
     * @return wrapped {@link Done} outcome
     */
    static <R> Outcome<R> done(R value) {
        return new Done<>(value);
    }

    /**
     * ➡️ Marker outcome meaning "proceed with the next link".
     */
    final class Continue<R> implements Outcome<R> {
        /**
         * ♻️ Shared singleton instance.
         */
        static final Continue<?> INSTANCE = new Continue<>();

        private Continue() {
        }
    }

    /**
     * ✅ @return {@code true} if this outcome is {@link Outcome.Done}.
     */
    default boolean isDone() {
        return this instanceof Outcome.Done<?>;
    }

    /**
     * 🔄 Transform the value if this is a {@link Outcome.Done}.
     *
     * <p>If not done, propagates {@link Outcome#next()}.</p>
     *
     * @param mapper function to map result
     * @param <T>    new result type
     * @return mapped outcome or {@link Outcome#next()}
     */
    default <T> Outcome<T> map(Function<? super R, ? extends T> mapper) {
        return this instanceof Done<R>(R value)
                ? Outcome.done(mapper.apply(value)) : Outcome.next();
    }

    /**
     * 🪃 Extract value if {@link Outcome.Done}, otherwise compute fallback.
     *
     * <p>Unlike {@code orElse}, the fallback is a
     * {@link BiFunction} with access to context and input.</p>
     *
     * @param function fallback producer
     * @param c  context
     * @param i  input
     * @return value or fallback result
     */
    default R orElseGet(BiFunction<?, ?, ? extends R> function, Object c, Object i) {
        if (this instanceof Done<R>(R value)) {
            return value;
        }

        @SuppressWarnings("unchecked")
        BiFunction<Object, Object, ? extends R> mapper =
                (BiFunction<Object, Object, ? extends R>) function;

        return mapper.apply(c, i);
    }


    /**
     * ✅ Final outcome carrying the completed value.
     *
     * <p>Short-circuits the chain immediately.</p>
     *
     * @param value resolved result
     */
    record Done<R>(R value) implements Outcome<R> { }

}
