package org.jmouse.access.el.condition;

import org.jmouse.access.AuthenticationFacts;
import org.jmouse.access.Moments;
import org.jmouse.access.TimeSpan;
import org.jmouse.access.spi.ConditionContext;
import org.jmouse.access.spi.DeferredValue;
import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * {@code caller is fresh('15m')} — whether the caller proved who they are recently enough.
 *
 * <p><em>Sudo mode</em>: a destructive action asks somebody to prove it is still them, rather than
 * trusting a session opened this morning and left on a train.
 *
 * <pre>
 * &#64;SPACE:'id-space-01' account:delete deny when caller is not fresh('15m')
 * &#64;SPACE:'id-space-01' key:rotate deny     when caller is not fresh('5m')
 * </pre>
 *
 * <p>It reads {@link AuthenticationFacts#AUTHENTICATED_AT} off the decision it is running inside — the
 * installation publishes it, this reaches no session and no request of its own.
 *
 * <h2>⚠️ It ignores its left-hand side</h2>
 *
 * <p>{@code caller} is there because the sentence needs a subject; the fact is ambient and the operand is
 * unused. Same shape as {@link WorkingHoursTest}, same reason, and the same eventual fix — a descriptor
 * saying what a test is about.
 *
 * <h2>⚠️ An absent fact refuses, and that is the design</h2>
 *
 * <p>If nothing publishes an authentication time, this throws, so {@code ConditionAxis} applies the deny
 * and drops the allow. Loud, and correct: reading absence as <em>"fresh"</em> would mean a sudo-mode rule
 * silently protecting nothing the day somebody reorganises the publishers.
 *
 * <h2>⚠️ An agent can never be fresh, and that is a feature</h2>
 *
 * <p>A protocol call carries no interactive authentication to be recent — there is nothing for the caller
 * to re-prove. So this refuses for an agent, and a sudo-mode rule puts destructive actions out of an
 * agent's reach without anybody writing a second rule about agents.
 *
 * <p>⚠️ It is a trap in the other direction too: a scheduled job or an internal caller hits the same
 * refusal. A rule guarded by {@code fresh(…)} is a rule about <strong>people at a keyboard</strong>, and
 * belongs only on actions that make sense there.
 */
public class FreshTest implements AccessTest {

    public static final String NAME = "fresh";

    private final Clock clock;

    public FreshTest() {
        this(Clock.systemDefaultZone());
    }

    public FreshTest(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        TimeSpan span = TimeSpan.parse(required(arguments));

        // ⚠️ Resolved, not read raw. An ambient value may be a supplier, and comparing a lambda against a
        // moment would answer something rather than fail.
        ConditionContext decision  = ConditionBinding.require(context);
        Object           published = DeferredValue.resolve(decision.value(AuthenticationFacts.AUTHENTICATED_AT));

        if (published == null) {
            throw new IllegalStateException(
                    ("nothing published '%s', so there is no way to tell how long ago this caller proved "
                     + "who they are. An installation publishes it as an ambient value; a caller with no "
                     + "interactive sign-in to re-prove — an agent, a scheduled job — never has one, and "
                     + "refusing is the right answer for both.")
                            .formatted(AuthenticationFacts.AUTHENTICATED_AT));
        }

        Instant  proven  = Moments.read(published, clock.getZone());
        Duration elapsed = Duration.between(proven, Instant.now(clock));

        // Inclusive, and a moment in the future counts as fresh — clock skew between the token's issuer
        // and this machine is ordinary. Matches WithinTest, deliberately.
        return elapsed.compareTo(span.length()) <= 0;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void verifyArguments(List<String> arguments) {
        if (arguments.isEmpty() || arguments.get(0) == null) {
            return;
        }

        TimeSpan.parse(arguments.get(0));
    }

    private static String required(Arguments arguments) {
        if (arguments == null || arguments.isEmpty() || arguments.getFirst() == null) {
            throw new IllegalArgumentException(
                    "fresh needs a span to compare against — for example `caller is fresh('15m')`");
        }

        return String.valueOf(arguments.getFirst());
    }
}
