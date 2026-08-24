package org.jmouse.access.el.condition;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * {@code now(part)} — the clock, as a value a rule may compare.
 *
 * <p>The first thing anybody asks an authorization rule and, until this, could not:
 *
 * <pre>
 * &#64;SPACE:'id-space-01' shipment:write deny  when now('hour') >= 18
 * &#64;SPACE:'id-space-01' release:publish deny when now('weekday') >= 6
 * &#64;SPACE:'id-space-01' report:read allow     when now('time') >= '09:00' and now('time') &lt; '18:00'
 * </pre>
 *
 * <p>The parts, and why each is shaped the way it is, are in {@link MomentPart}. A part this does not
 * know is refused <strong>at load</strong>, listing the ones that would have worked — a typo that only
 * failed at evaluation would boot clean and then refuse everybody, which is the failure
 * {@code ConditionCalls} exists to prevent.
 *
 * <h2>⚠️ The zone is the installation's, not the caller's</h2>
 *
 * <p>A rule saying <em>after 18:00</em> means 18:00 where the installation is. The caller's zone is not
 * something the engine knows, and inventing one would make the same rule mean different things for two
 * people looking at the same screen. Pass a {@link Clock} — never call {@code LocalDateTime.now()}
 * inline — so an installation in another zone and a test at a fixed instant are the same code path.
 *
 * <h2>⚠️ It does not fail open</h2>
 *
 * <p>A clock does not go down, so this can barely fail at all. It still declines to declare
 * {@link AccessFunction#failsOpen()}: if reading the time ever did throw, the answer that <em>refuses</em>
 * is the safe half, and a working-hours rule that waves everybody through because the clock hiccuped is
 * exactly the shape this engine is careful about.
 */
public class NowFunction implements AccessFunction {

    public static final String NAME = "now";

    private final Clock clock;

    public NowFunction() {
        this(Clock.systemDefaultZone());
    }

    public NowFunction(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException(
                    "now(part) needs to say which part of the moment it means — for example now('hour')");
        }

        Object written = arguments.getFirst();

        return MomentPart.parse(written == null ? null : String.valueOf(written)).read(moment());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void verifyArguments(List<String> arguments) {
        // A part written as anything but a literal — now(somePart) — arrives as null, and the honest
        // answer is to decline to check rather than to guess. The same bargain ConsumedFunction makes.
        if (arguments.isEmpty() || arguments.get(0) == null) {
            return;
        }

        MomentPart.parse(arguments.get(0));
    }

    /** The moment, exposed so a test that pins a clock is testing this and not a private detail. */
    protected ZonedDateTime moment() {
        return ZonedDateTime.now(clock);
    }
}
