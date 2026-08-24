package org.jmouse.access.el.condition;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * {@code now is workingHours} — whether the installation is open right now.
 *
 * <pre>
 * &#64;SPACE:'id-space-01' shipment:write deny when now is not workingHours
 * &#64;SPACE:'id-space-01' report:export allow  when now is workingHours('mon-fri 09:00-18:00')
 * </pre>
 *
 * <p>Bare, it reads the installation's own schedule. With a literal argument it reads the one written in
 * the rule, which is how two rules in one file can disagree about opening hours on purpose — a warehouse
 * and a support desk keep different ones.
 *
 * <p>The schedule's spelling, and why its end is exclusive, are on {@link WorkingSchedule}. A schedule
 * that will not parse is refused <strong>at load</strong>, so a typo fails the boot rather than becoming
 * a rule that never holds.
 *
 * <h2>⚠️ It ignores what stands to its left</h2>
 *
 * <p>{@code now is workingHours} reads as though {@code now} were the subject. It is not: this asks the
 * {@link Clock} it was built with, and the operand is unused. {@code now} is there because the sentence
 * needs a subject to be English.
 *
 * <p>Which means {@code caller is workingHours} also compiles and answers the same thing, which is
 * nonsense that loads clean. That is accepted deliberately, for now: refusing an operand would need a way
 * for a test to say <em>what it is about</em>, and that is a descriptor rather than a special case here.
 * If a second clock-shaped test turns up, build the descriptor.
 *
 * <h2>⚠️ The zone is the installation's</h2>
 *
 * <p>Same as {@link NowFunction}, and for the same reason: "we close at six" means six where the
 * installation runs. The caller's own zone is not something the engine knows, and inventing one would
 * make a single rule mean different things for two people looking at the same screen.
 *
 * <h2>⚠️ It does not fail open</h2>
 *
 * <p>A schedule that cannot be read refuses. {@link AccessTest#failsOpen()} stays {@code false}, so the
 * axis applies a deny whose schedule would not parse and drops an allow — the half that says no.
 */
public class WorkingHoursTest implements AccessTest {

    public static final String NAME = "workingHours";

    private final Clock           clock;
    private final WorkingSchedule schedule;

    public WorkingHoursTest() {
        this(Clock.systemDefaultZone(), WorkingSchedule.standard());
    }

    public WorkingHoursTest(Clock clock) {
        this(clock, WorkingSchedule.standard());
    }

    public WorkingHoursTest(Clock clock, WorkingSchedule schedule) {
        this.clock    = clock == null ? Clock.systemDefaultZone() : clock;
        this.schedule = schedule == null ? WorkingSchedule.standard() : schedule;
    }

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        return scheduleFor(arguments).covers(moment());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void verifyArguments(List<String> arguments) {
        // A schedule written as anything but a literal cannot be read here, and the honest answer is to
        // decline to check rather than to guess — the same bargain every other verifyArguments makes.
        if (arguments.isEmpty() || arguments.get(0) == null) {
            return;
        }

        // Throws with its own sentence naming the spelling that works.
        WorkingSchedule.parse(arguments.get(0));
    }

    /** The installation's schedule, or the one this rule wrote. */
    private WorkingSchedule scheduleFor(Arguments arguments) {
        if (arguments == null || arguments.isEmpty() || arguments.getFirst() == null) {
            return schedule;
        }

        return WorkingSchedule.parse(String.valueOf(arguments.getFirst()));
    }

    /** The moment, exposed so a test pinning a clock is testing this and not a private detail. */
    protected ZonedDateTime moment() {
        return ZonedDateTime.now(clock);
    }
}
