package org.jmouse.access.el.condition;

import org.jmouse.access.Moments;
import org.jmouse.access.TimeSpan;
import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Everything {@code olderThan} and {@code within} have in common — which is all of it except one
 * comparison.
 *
 * <h2>Why these two exist</h2>
 *
 * <p>⚠️ A condition has <strong>no arithmetic</strong>. {@code ConditionVocabulary} admits no {@code +},
 * {@code -}, {@code *} or {@code /}, so <em>"how long ago"</em> is not expressible in a rule — not
 * awkwardly, not verbosely, not at all. These two tests are the whole answer to that, which is why they
 * were built before the functions that merely read something more conveniently.
 *
 * <pre>
 * &#64;SPACE:'id-space-01' comment:edit allow    when resource.createdAt is within('15m')
 * &#64;SPACE:'id-space-01' invoice:void deny    when resource.createdAt is olderThan('30d')
 * </pre>
 *
 * <h2>⚠️ The two are not complements</h2>
 *
 * <table>
 *   <tr><th></th><th>in the future</th><th>unreadable</th><th>absent</th></tr>
 *   <tr><td>{@code within}</td><td>true</td><td>throws</td><td>throws</td></tr>
 *   <tr><td>{@code olderThan}</td><td>false</td><td>throws</td><td>throws</td></tr>
 * </table>
 *
 * <p>Neither answers {@code false} for <em>"I could not tell"</em>. A {@code false} inside a {@code deny}
 * <strong>permits</strong>, so a retention rule whose timestamp was null would quietly stop refusing.
 * Throwing carries it to {@code ConditionAxis}, which applies the deny and drops the allow.
 *
 * <p>⚠️ A moment <strong>in the future</strong> is not an error — clock skew between machines is
 * ordinary — so {@code within} holds and {@code olderThan} does not. The opposite reading is equally
 * defensible, which is exactly why it is written down.
 */
public abstract class AgeTest implements AccessTest {

    private final Clock clock;

    protected AgeTest(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        TimeSpan span    = TimeSpan.parse(required(arguments));
        Instant  moment  = Moments.read(value, clock.getZone());
        Duration elapsed = Duration.between(moment, Instant.now(clock));

        return decide(elapsed, span.length());
    }

    @Override
    public void verifyArguments(List<String> arguments) {
        // A span written as anything but a literal cannot be read here, and the honest answer is to
        // decline to check rather than to guess — the same bargain every other verifyArguments makes.
        if (arguments.isEmpty() || arguments.get(0) == null) {
            return;
        }

        // Throws with its own sentence naming the spelling that works.
        TimeSpan.parse(arguments.get(0));
    }

    /**
     * @param elapsed how long ago the moment was — ⚠️ <strong>negative</strong> for one in the future
     * @param span    the length the rule wrote
     */
    protected abstract boolean decide(Duration elapsed, Duration span);

    private String required(Arguments arguments) {
        if (arguments == null || arguments.isEmpty() || arguments.getFirst() == null) {
            throw new IllegalArgumentException(
                    "%s needs a span to compare against — for example `is %s('15m')`"
                            .formatted(getName(), getName()));
        }

        return String.valueOf(arguments.getFirst());
    }
}
