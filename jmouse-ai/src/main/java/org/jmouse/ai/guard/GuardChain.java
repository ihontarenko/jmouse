package org.jmouse.ai.guard;

import org.jmouse.ai.CallVerdict;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolInvocation;
import org.jmouse.core.SecureRandomStringGenerator;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Everything that stands between a language model and a caller's data, in order.
 *
 * <p>The five shipped guards address five different ways an assistant goes wrong, and each is
 * independently sufficient to stop the one it addresses: a loop quietly producing hundreds of valid
 * calls, one call reaching far more records than anyone intended, a filter that matched nothing being
 * mistaken for success, a wrong intent acted on before anyone saw it, and the same call arriving
 * twice.
 *
 * <p><strong>The rate limit applies to every call; the other four only to writes.</strong> A loop is a
 * loop whether or not it changes anything — a read hammered a thousand times is still a runaway caller
 * — while a read has no blast radius to bound, nothing to preview and nothing to duplicate.
 *
 * <h2>The order, and why each position is load-bearing</h2>
 *
 * <p><strong>Rate limit first</strong>, because it is the cheapest and a throttled caller should not
 * do database work to discover that it is throttled.
 *
 * <p><strong>A redemption skips the middle three entirely.</strong> A call carrying a valid
 * confirmation token was ceiling-checked, emptiness-checked and deduplicated when the token was
 * issued; asking again would mean resolving the records a second time, and re-resolving is precisely
 * the bug the frozen record set exists to prevent. Expressed here as three guards answering "this does
 * not apply to me" rather than as confirmation running earlier and short-circuiting — the behaviour is
 * identical, and putting the decision in each guard is what lets a product remove one without
 * disturbing the others.
 *
 * <p><strong>Ceiling before confirmation</strong>, because the two are constantly confused and must
 * not be: <em>confirmation asks whether you meant it; the ceiling says an operation that size is not
 * available at all.</em> No amount of agreeing gets past a ceiling, which is exactly why it is not
 * merely a larger threshold, and a caller offered a token for something that will be refused anyway
 * learns the wrong lesson.
 *
 * <p><strong>Empty destruction before confirmation</strong>, because otherwise a filter that matched
 * nothing produces a preview of zero records and a token to confirm it with — which a model reads as
 * "ready to proceed" and a person reads as "it worked".
 *
 * <p><strong>Deduplication last</strong>, after the call is known to be permitted by everything else,
 * so a refused call is never remembered as a completed one.
 *
 * <p>A product contributes a sixth guard by adding it to the list; the gaps between the shipped orders
 * are wide enough to slot one between any two. A product that wants none passes an empty list, and the
 * dispatcher still works.
 */
public final class GuardChain {

    /** Long enough to be unique across a trail, short enough to quote in a log line. */
    private static final int OPERATION_ID_LENGTH = 16;

    private final List<InvocationGuard>       guards;
    private final SecureRandomStringGenerator operationIdentifiers;

    public GuardChain(List<InvocationGuard> guards) {
        this.guards               = guards.stream().sorted(Comparator.comparingInt(InvocationGuard::order)).toList();
        this.operationIdentifiers = new SecureRandomStringGenerator(OPERATION_ID_LENGTH);
    }

    /** No guards at all — a product that has not decided, or one behind something that already does this. */
    public static GuardChain empty() {
        return new GuardChain(List.of());
    }

    /** The five, wired to the in-memory stores and a local bucket. What a sandbox and a first draft want. */
    public static GuardChain defaults() {
        GuardSettings settings = GuardSettings.defaults();

        return new GuardChain(List.of(
                new RateLimitGuard(TokenBucketCallerRateLimiter.defaults()),
                new CeilingGuard(settings),
                new EmptyDestructionGuard(),
                new ConfirmationGuard(new InMemoryConfirmationStore(settings.confirmationLifetime()), settings),
                new DeduplicationGuard(new InMemoryDuplicateCallStore(settings.deduplicationWindow()))));
    }

    /**
     * Runs one call through every guard that applies to it and then, only if all of them are
     * satisfied, runs it.
     *
     * @param work the action's own handler, invoked with whatever record set the guards froze
     * @return what the handler produced, or what stood in place of doing the work, along with which of
     *         those it was and what it reached
     * @throws org.jmouse.ai.ToolRefusedException when a guard stops the call
     */
    public GuardedCall run(
            ToolAction action, ToolInvocation invocation, Function<ToolInvocation, Object> work) {

        GuardContext context = new GuardContext(
                action, invocation,
                operationIdentifiers.generate(),
                CallFingerprint.of(invocation.callerId(), action.publishedName(), invocation.arguments()));

        return continueFrom(0, work).proceed(context);
    }

    /** Which guards exist, for the startup check that a configured one is not missing. */
    public Set<String> names() {
        return guards.stream().map(InvocationGuard::name).collect(Collectors.toSet());
    }

    /**
     * The continuation standing at position {@code index}.
     *
     * <p>Built lazily rather than composed up front, so that a guard's decision not to apply costs
     * nothing and the stack a failure produces names the guards a call actually passed through.
     */
    private GuardContinuation continueFrom(int index, Function<ToolInvocation, Object> work) {
        if (index >= guards.size()) {
            return context -> carryOut(context, work);
        }

        InvocationGuard guard = guards.get(index);

        return context -> {
            GuardContinuation next = continueFrom(index + 1, work);
            return guard.appliesTo(context) ? guard.guard(context, next) : next.proceed(context);
        };
    }

    /**
     * The end of the chain: the work itself.
     *
     * <p>Freezes whatever was resolved immediately beforehand, so a handler receives its records even
     * in a chain configured without the confirmation guard. Tying the freeze to that guard would make
     * removing it silently change what every write handler is given.
     */
    private GuardedCall carryOut(GuardContext context, Function<ToolInvocation, Object> work) {
        context.freezeResolved();

        Object payload = work.apply(context.invocation());

        return new GuardedCall(
                payload, CallVerdict.CARRIED_OUT, context.operationId(),
                context.reachedRecords(), context.reachedCount(), context.throughConfirmation());
    }
}
