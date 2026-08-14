package org.jmouse.ai.guard;

/**
 * The rest of the chain, from where one guard is standing.
 *
 * <p>Its own type rather than a {@code Supplier} so that a guard's signature says what not calling it
 * means: the guard has answered, and the work will not happen. That is the ordinary case for two of
 * the shipped five, and it should read as ordinary.
 */
@FunctionalInterface
public interface GuardContinuation {

    /** Runs the remaining guards and, if all of them let it, the work. */
    GuardedCall proceed(GuardContext context);
}
