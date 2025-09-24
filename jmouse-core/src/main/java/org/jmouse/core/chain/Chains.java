package org.jmouse.core.chain;

/**
 * 🧩 Utility helpers for working with {@link Chain}.
 */
public class Chains {

    private Chains() {
    }

    /**
     * ⏱️ Wraps a chain to measure execution time.
     *
     * <p>On {@link Outcome.Done} it returns a {@link TimedResult}
     * containing the value and elapsed nanoseconds.</p>
     *
     * <p>If the chain continues, this wrapper also continues
     * without recording a result.</p>
     *
     * @param chain original chain
     * @param <C>   context type
     * @param <I>   input type
     * @param <R>   result type
     * @return chain producing {@link TimedResult}
     */
    public static <C, I, R> Chain<C, I, TimedResult<R>> timed(Chain<C, I, R> chain) {
        return (context, input) -> {
            long       t0      = System.nanoTime();
            Outcome<R> outcome = chain.proceed(context, input);
            long       t1      = System.nanoTime();

            if (outcome instanceof Outcome.Done<R>(R value)) {
                return Outcome.done(new TimedResult<>(value, t1 - t0));
            }

            return Outcome.next();
        };
    }

    /**
     * 📊 Result wrapper with measured duration.
     *
     * @param value actual result
     * @param nanos elapsed time in nanoseconds
     * @param <R>   result type
     */
    public record TimedResult<R>(R value, long nanos) { }

}
