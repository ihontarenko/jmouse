package org.jmouse.core.chain;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 🔗 Factory methods for common {@link Link} implementations.
 */
public final class Links {

    private Links() {}

    /**
     * 🕵️ Side-effect hook that observes input before continuing.
     *
     * <p>Always delegates to the next link with {@link Outcome#next()}.</p>
     *
     * @param spy consumer for context and input
     * @param <C> context type
     * @param <I> input type
     * @param <R> result type
     * @return link that only taps into the chain
     */
    public static <C, I, R> Link<C, I, R> tap(BiConsumer<C, I> spy) {
        return (context, input, next) -> {
            spy.accept(context, input);
            return Outcome.next();
        };
    }

    /**
     * 🏁 Side-effect hook that runs after chain completion.
     *
     * <p>Always executes the spy in a {@code finally} block,
     * regardless of success or exception.</p>
     *
     * @param spy consumer for context and input
     * @param <C> context type
     * @param <I> input type
     * @param <R> result type
     * @return link that finalizes execution
     */
    public static <C, I, R> Link<C, I, R> finalize(BiConsumer<C, I> spy) {
        return (context, input, next) -> {
            try {
                return next.proceed(context, input);
            } finally {
                spy.accept(context, input);
            }
        };
    }

    /**
     * ⚖️ Conditional branch: produce result if predicate matches.
     *
     * <p>If {@code predicate} is true, short-circuits the chain
     * with {@link Outcome#done(Object)}. Otherwise continues.</p>
     *
     * @param predicate condition to test input
     * @param mapper    function to map input to result
     * @param <C>       context type
     * @param <I>       input type
     * @param <R>       result type
     * @return conditional link
     */
    public static <C, I, R> Link<C, I, R> when(Predicate<I> predicate, Function<I, R> mapper) {
        return (context, input, next) ->
                predicate.test(input) ? Outcome.done(mapper.apply(input)) : Outcome.next();
    }

}
