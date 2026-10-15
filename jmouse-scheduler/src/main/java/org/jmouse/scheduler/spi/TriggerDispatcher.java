package org.jmouse.scheduler.spi;

import org.jmouse.scheduler.Occurrence;

/**
 * Hands a claimed, gated occurrence on to where its work actually runs. The product implements this:
 * a product with a durable job queue enqueues a job here and lets its own worker run the handler;
 * {@link org.jmouse.scheduler.InProcessDispatcher} runs the handler on a thread of this process.
 *
 * <p>⚠️ <strong>Must be idempotent on {@link Occurrence#identity()}.</strong> Firing is at-least-once:
 * when a runner dies between dispatching and confirming, another runner dispatches the same occurrence
 * again once the lease expires. A queue-backed dispatcher puts a unique constraint on the identity and
 * treats a duplicate as success.
 *
 * <p>⚠️ The runner calls this on its single polling thread, within the lease, so it should return
 * quickly — enqueue, do not work. A dispatcher that throws records the occurrence as
 * {@link org.jmouse.scheduler.OccurrenceOutcome#FAILED}, and the trigger still moves on: a failure that
 * returns is not retried; only a runner that never returns is.
 */
@FunctionalInterface
public interface TriggerDispatcher {

    /**
     * @param occurrence the occurrence to run; its {@link Occurrence#identity()} is the idempotency key
     */
    void dispatch(Occurrence occurrence);
}
