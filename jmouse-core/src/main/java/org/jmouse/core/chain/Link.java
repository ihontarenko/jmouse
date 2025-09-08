package org.jmouse.core.chain;

/**
 * 🔗 Single element in a {@link Chain} (Chain of Responsibility).
 *
 * <p>Each link can:
 * <ul>
 *   <li>✅ Handle the input and return a non-null result (short-circuit the chain)</li>
 *   <li>➡️ Delegate to the {@code next} element if it cannot fully handle</li>
 *   <li>🏳️ Return {@code null} to indicate "no result, continue chain"</li>
 * </ul>
 * </p>
 *
 * @param <C> context type (e.g. request, environment, state)
 * @param <I> input type (payload to process)
 * @param <R> result type (may be {@code null} to continue)
 */
@FunctionalInterface
public interface Link<C, I, R> {

    /**
     * ⚡ Handle input or delegate to the next link.
     *
     * @param context  processing context
     * @param input   input payload
     * @param next the remaining chain
     * @return non-null result to short-circuit, or {@code null} to continue
     */
    Outcome<R> handle(C context, I input, Chain<C, I, R> next);
}
