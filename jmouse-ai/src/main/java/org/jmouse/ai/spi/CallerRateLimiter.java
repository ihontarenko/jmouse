package org.jmouse.ai.spi;

/**
 * May this caller call again yet?
 *
 * <p>Guards against a loop, not against an adversary. An assistant that misreads its own last result
 * and calls again, and again, produces hundreds of individually valid calls — every one permitted,
 * every one within its ceiling, and the whole sequence obviously wrong to anybody watching. The bucket
 * is what notices, because nothing else in the chain is looking at the sequence.
 *
 * <p>A seam because the answer is genuinely different per deployment: one application counts in a
 * local bucket, another shares one across a cluster through a cache, and a third has an edge in front
 * of it that already does this. None of those is a default the library could pick.
 *
 * <p>Named for what it is keyed by. {@code jmouse-core} has a sealed {@code RateLimiter} of its own
 * that answers a related question with no key at all, and an implementation here is very likely to be
 * written over one — a file that had to qualify both names would be paying for a collision that a
 * more accurate name avoids.
 */
public interface CallerRateLimiter {

    /**
     * Takes one permit, or says there is none.
     *
     * <p>Returns rather than throws, so that the refusal — which must be written for a model that will
     * otherwise retry immediately — is composed in one place with everything else the guard knows.
     */
    boolean tryAcquire(String callerId);

    /**
     * The allowance, in words, for the refusal that has to say what it is.
     *
     * <p>A throttled caller told only "too fast" retries; one told "sixty calls a minute, refilling
     * continuously" can wait. Worth the method.
     */
    default String describeAllowance() {
        return "a fixed allowance per caller, refilling continuously";
    }

    /**
     * No limiting at all.
     *
     * <p>The honest default, and it is the permissive one for the same reason every other seam's is: a
     * product must be able to run a tool before it has decided any of this. It is also the one default
     * a product should be in a hurry to replace, because the failure it does not stop is the failure
     * that produces the largest bill.
     */
    static CallerRateLimiter permitAll() {
        return new CallerRateLimiter() {

            @Override
            public boolean tryAcquire(String callerId) {
                return true;
            }

            @Override
            public String describeAllowance() {
                return "no limit is configured";
            }
        };
    }
}
