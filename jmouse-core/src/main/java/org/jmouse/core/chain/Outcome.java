package org.jmouse.core.chain;

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
     * ✅ Final outcome carrying the completed value.
     *
     * <p>Short-circuits the chain immediately.</p>
     *
     * @param value resolved result
     */
    record Done<R>(R value) implements Outcome<R> { }

}
