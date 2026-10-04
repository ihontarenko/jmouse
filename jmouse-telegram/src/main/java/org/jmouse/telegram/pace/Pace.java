package org.jmouse.telegram.pace;

import org.jmouse.telegram.ChatReference;

import java.time.Duration;

/**
 * How fast we are allowed to talk to Telegram.
 *
 * <h2>⚠️ It waits rather than refusing, and that is the whole difference from a rate limiter</h2>
 *
 * <p>{@code jmouse-ai}'s {@code CallerRateLimiter} answers "may this call happen?" with a boolean,
 * because the thing it guards against is a model in a loop and the right answer to that is <em>no</em>.
 * Here the caller is a notification that somebody is waiting for, and the right answer to "not yet" is
 * to wait: dropping it would lose the message, and sending it anyway earns a flood penalty that delays
 * every <em>other</em> message too.
 *
 * <h2>Two limits, because Telegram enforces two</h2>
 *
 * <p>Roughly thirty messages a second overall, and roughly twenty a minute into any one group. A
 * policy that models only the first passes a bulk send to one group straight into the second.
 *
 * <p>⚠️ Both figures are Telegram's <em>published guidance</em> rather than a documented contract —
 * they are not returned by any endpoint and have been described differently at different times. So
 * this is configuration, not a constant, and {@link #penalise} exists because the authoritative
 * number is the one Telegram gives when it is actually unhappy.
 */
public interface Pace {

    /**
     * Blocks until this call is allowed to go out.
     *
     * <p>⚠️ Interruptible. A polling loop or an outbox worker is shut down by interrupting its thread,
     * and a pace that swallowed the interrupt would be a thread that will not stop.
     *
     * @throws java.util.concurrent.CancellationException if the thread was interrupted while waiting
     */
    void awaitTurn(ChatReference chat);

    /**
     * Telegram said we were too fast, and said for how long.
     *
     * <p>⚠️ Its figure is authoritative and replaces whatever this policy believed. A bucket that goes
     * on issuing permits during a penalty produces a burst of calls that are all refused, each of
     * which extends the penalty.
     */
    void penalise(ChatReference chat, Duration retryAfter);

    /**
     * No pacing at all.
     *
     * <p>Honest for a smoke class and for an application that sends a handful of messages a day, and
     * the wrong choice for anything that sends in bulk — which is the only shape that hits the limits
     * in the first place.
     */
    static Pace unlimited() {
        return new Pace() {

            @Override
            public void awaitTurn(ChatReference chat) {
                // Nothing to wait for.
            }

            @Override
            public void penalise(ChatReference chat, Duration retryAfter) {
                // Nothing to remember; the caller still honours the delay it was given.
            }
        };
    }
}
