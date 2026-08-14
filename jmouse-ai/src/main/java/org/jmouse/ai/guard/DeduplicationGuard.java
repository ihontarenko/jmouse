package org.jmouse.ai.guard;

import org.jmouse.ai.CallVerdict;
import org.jmouse.ai.ToolInvocation;
import org.jmouse.ai.spi.DuplicateCallStore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Stops the same write happening twice.
 *
 * <p>A transport retries, a model does not see a response, a user asks twice in one breath — and a
 * second record appears that nobody wanted and nobody notices until an inventory is wrong. The window
 * is sized for exactly those, not for somebody genuinely adding two of something, which is what
 * {@code allowDuplicate} is for.
 *
 * <p><strong>Last in the chain, after the call is known to be permitted by everything else.</strong>
 * Remembering earlier would let a refused call be remembered as a completed one, so that its retry —
 * which might be the corrected version — comes back with the first attempt's result.
 *
 * <p>The suppressed answer carries what the first call produced rather than an error, because that is
 * what a model asked "did that work?" needs: the identifier of the thing that already exists. It also
 * says plainly that this call did nothing, so a model does not report two records where there is one.
 *
 * <p>Does not apply to a call redeeming a confirmation token. A token works once, so a confirmed call
 * cannot arrive twice, and the store has nothing to add.
 */
public final class DeduplicationGuard implements InvocationGuard {

    public static final String NAME = "deduplication";

    /** What a suppressed call answers with, which a transport and a model both branch on. */
    public static final String DUPLICATE_SUPPRESSED = "duplicate-suppressed";

    private final DuplicateCallStore duplicateCalls;

    public DeduplicationGuard(DuplicateCallStore duplicateCalls) {
        this.duplicateCalls = duplicateCalls;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int order() {
        return DEDUPLICATION_ORDER;
    }

    @Override
    public boolean appliesTo(GuardContext context) {
        return context.action().writes()
            && !context.redeemsConfirmation()
            && !context.invocation().flag(ToolInvocation.ALLOW_DUPLICATE_ARGUMENT);
    }

    @Override
    public GuardedCall guard(GuardContext context, GuardContinuation next) {
        Optional<Object> alreadyDone = duplicateCalls.findResult(context.fingerprint());

        if (alreadyDone.isPresent()) {
            return suppressed(context, alreadyDone.get());
        }

        GuardedCall carried = next.proceed(context);

        // Only what actually ran. A preview changed nothing and must be repeatable, or a caller that
        // looked at one and thought better of it could never look again.
        if (carried.verdict() == CallVerdict.CARRIED_OUT) {
            duplicateCalls.remember(context.fingerprint(), carried.payload());
        }

        return carried;
    }

    private GuardedCall suppressed(GuardContext context, Object previousResult) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("status", DUPLICATE_SUPPRESSED);
        body.put("message",
                "An identical call was made within the last " + duplicateCalls.window().toSeconds()
                + " seconds, so this one did nothing and what that call produced is below. If a second, "
                + "genuinely separate record is wanted, call again with "
                + ToolInvocation.ALLOW_DUPLICATE_ARGUMENT + "=true.");
        body.put("previousResult", previousResult);

        return new GuardedCall(body, CallVerdict.DUPLICATE_SUPPRESSED, context.operationId(),
                context.reachedRecords(), context.reachedCount(), false);
    }
}
