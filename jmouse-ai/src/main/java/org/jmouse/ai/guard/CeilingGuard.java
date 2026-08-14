package org.jmouse.ai.guard;

import org.jmouse.ai.AffectedRecords;
import org.jmouse.ai.RefusalReason;
import org.jmouse.ai.ToolRefusedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bounds the blast radius of one call.
 *
 * <p>⚠️ <strong>The ceiling is not a larger confirmation threshold, and the refusal must say so.</strong>
 * Confirmation asks whether you meant it; the ceiling says an operation that size is not available at
 * all. No amount of agreeing gets past it. A model that reads this refusal as "ask again with confirm"
 * will do exactly that, so the message states plainly that this is a refusal and offers the only thing
 * that will actually work: narrowing the call.
 *
 * <p>Reads {@link AffectedRecords#totalCount()} and never the records, which is why a call affecting
 * four thousand of them can be refused without four thousand rows being loaded to say so.
 *
 * <p>Does not apply to a call redeeming a confirmation token: that call was ceiling-checked when its
 * token was issued, and resolving its records again to check a second time is the bug the frozen
 * record set exists to prevent.
 */
public final class CeilingGuard implements InvocationGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(CeilingGuard.class);

    public static final String NAME = "ceiling";

    private final GuardSettings settings;

    public CeilingGuard(GuardSettings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int order() {
        return CEILING_ORDER;
    }

    @Override
    public boolean appliesTo(GuardContext context) {
        return context.action().writes() && !context.redeemsConfirmation();
    }

    @Override
    public GuardedCall guard(GuardContext context, GuardContinuation next) {
        long reached = context.affectedRecords().totalCount();
        long ceiling = settings.perCallCeiling();

        if (reached <= ceiling) {
            return next.proceed(context);
        }

        LOGGER.info("Refused {} — {} records over the ceiling of {}",
                context.action().qualifiedName(), reached, ceiling);

        throw new ToolRefusedException(RefusalReason.OVER_CEILING,
                "This request affects " + reached + " records and the maximum for one call is " + ceiling
                + ". Narrow it — name a single target, add something to match on, or pass the specific "
                + "records you mean — and call again. This is a refusal, not a confirmation: an "
                + "operation this size is not available at any level of agreement. Nothing was changed.");
    }
}
