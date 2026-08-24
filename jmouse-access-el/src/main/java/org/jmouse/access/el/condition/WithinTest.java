package org.jmouse.access.el.condition;

import java.time.Clock;
import java.time.Duration;

/**
 * {@code X is within('15m')} — whether the moment on the left is no further back than the span.
 *
 * <pre>
 * &#64;SPACE:'id-space-01' comment:edit allow    when resource.createdAt is within('15m')
 * &#64;SPACE:'id-space-01' article:publish deny when caller.registeredAt is within('7d')
 * </pre>
 *
 * <p>Edit windows, grace periods, cooling-off periods, and the anti-abuse rule that keeps a brand-new
 * account from doing the thing spammers do first.
 *
 * <p>⚠️ Inclusive: a moment exactly the span ago still holds. Paired with {@link OlderThanTest}'s strict
 * comparison, the two partition a timeline with no gap and no overlap.
 *
 * <p>The rest — what happens to a future moment, an unreadable one and an absent one — is on
 * {@link AgeTest}, and is worth reading before writing a rule with either.
 */
public class WithinTest extends AgeTest {

    public static final String NAME = "within";

    public WithinTest() {
        this(Clock.systemDefaultZone());
    }

    public WithinTest(Clock clock) {
        super(clock);
    }

    @Override
    protected boolean decide(Duration elapsed, Duration span) {
        return elapsed.compareTo(span) <= 0;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
