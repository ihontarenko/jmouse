package org.jmouse.ai.guard;

import org.jmouse.ai.RefusalReason;
import org.jmouse.ai.ToolRefusedException;
import org.jmouse.ai.spi.CallerRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stops a loop.
 *
 * <p>The one guard that applies to reads as well as writes. A read hammered a thousand times changes
 * nothing and is still a runaway caller, and nothing else in the chain is looking at the
 * <em>sequence</em> — every individual call is valid, permitted and within its ceiling.
 *
 * <p>First in the chain because it is the cheapest, and because a throttled caller should not be doing
 * database work to find out that it is throttled.
 *
 * <p>The refusal is written for a model that will otherwise retry immediately, so it says four things
 * a shorter one would not: what the allowance is, that it refills on its own, that nothing was
 * changed, and — most usefully — that if a loop is producing these calls then it is the loop that
 * needs stopping. A caller told only "too fast" retries, faster.
 */
public final class RateLimitGuard implements InvocationGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitGuard.class);

    public static final String NAME = "rate-limit";

    private final CallerRateLimiter rateLimiter;

    public RateLimitGuard(CallerRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int order() {
        return RATE_LIMIT_ORDER;
    }

    @Override
    public GuardedCall guard(GuardContext context, GuardContinuation next) {
        String callerId = context.invocation().callerId();

        if (rateLimiter.tryAcquire(callerId)) {
            return next.proceed(context);
        }

        LOGGER.info("Throttled {} for caller {}", context.action().qualifiedName(), callerId);

        throw new ToolRefusedException(RefusalReason.RATE_LIMITED,
                "This caller is calling too quickly and has been throttled. Its allowance is "
                + rateLimiter.describeAllowance() + ". Stop and wait rather than retrying — nothing was "
                + "changed, and the limit applies to this caller alone. If a loop is producing these "
                + "calls, it is the loop that needs stopping.");
    }
}
