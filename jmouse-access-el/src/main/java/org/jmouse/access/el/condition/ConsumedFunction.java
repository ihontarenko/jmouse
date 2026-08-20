package org.jmouse.access.el.condition;

import org.jmouse.access.CallerView;
import org.jmouse.access.ConsumptionKey;
import org.jmouse.access.ConsumptionWindow;
import org.jmouse.access.spi.ConsumptionCounters;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * {@code consumed('ai-token', '3h')} — how much the caller has used, so a rule can compare it.
 *
 * <p>The first {@link AccessFunction}, and the one the whole seam was opened for:
 *
 * <pre>{@code deny ai:ask when consumed('ai-token', '3h') >= 100000}</pre>
 *
 * <h2>⚠️ Why it returns a number instead of being a test</h2>
 *
 * <p>{@code caller is consumed('ai-token', '3h', 100000)} would have needed no change to the dialect at
 * all — tests with arguments already parsed. It is still the wrong shape, because a test can only
 * answer true or false, so the threshold becomes an argument the predicate interprets. As a function the
 * threshold is an ordinary comparison, written in the policy, legible to somebody who has never seen
 * this class. The number belongs to the rule, not to the predicate.
 *
 * <h2>The subject is the caller, unless a third argument says otherwise</h2>
 *
 * <p>{@code consumed(meter, window)} counts the person or agent making the request.
 * {@code consumed(meter, window, subjectKind, subjectId)} counts something else — a workspace, a
 * tenant — for the rules that are about a place rather than about whoever walked in.
 *
 * <h2>⚠️ It reads. It does not spend.</h2>
 *
 * <p>Conditions run on {@code ConditionAxis}, whose resolution is memoised per
 * {@code (subject, scope chain)} and whose one answer serves a page of rows. Spending here would spend
 * an unpredictable number of times. Recording stays a write after commit, made by whoever knows what
 * was actually consumed.
 *
 * <h2>⚠️ It fails closed</h2>
 *
 * <p>{@link #failsOpen()} is left at false deliberately. A counter store being unreachable is the
 * moment a quota matters most and is least able to prove itself, so the rule is applied rather than
 * ignored — a refusal somebody can complain about beats a bill nobody expected.
 */
public class ConsumedFunction implements AccessFunction {

    public static final String NAME = "consumed";

    /** What {@code caller} is called in a condition, and therefore where this reads the subject from. */
    private static final String CALLER = "caller";

    /** The subject kind a caller is counted under when a rule does not name one. */
    private final String                    callerKind;
    private final ConsumptionCounters       counters;
    private final Map<String, List<String>> recorded;
    private final Clock                     clock;

    /**
     * @param recorded which windows are actually written, per meter. ⚠️ Not decoration — see
     *                 {@link #verifyArguments(List)}. Empty means nothing can be checked
     */
    public ConsumedFunction(
            ConsumptionCounters counters, String callerKind, Map<String, List<String>> recorded) {

        this(counters, callerKind, recorded, Clock.systemDefaultZone());
    }

    public ConsumedFunction(
            ConsumptionCounters counters, String callerKind, Map<String, List<String>> recorded,
            Clock clock) {

        this.counters   = counters;
        this.callerKind = callerKind;
        this.recorded   = recorded == null ? Map.of() : Map.copyOf(recorded);
        this.clock      = clock;
    }

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        String meter  = required(arguments, 0, "meter");
        String window = required(arguments, 1, "window");

        String subjectKind = arguments.size() > 2 ? text(arguments.get(2)) : callerKind;
        String subjectId   = arguments.size() > 3 ? text(arguments.get(3)) : callerId(context);

        // ⚠️ Nobody signed in has consumed nothing, rather than being an error. A rule about usage on a
        // route reachable without an account should simply not hold — refusing there would refuse for a
        // reason the rule does not state.
        if (subjectId == null) {
            return 0L;
        }

        String windowKey = ConsumptionWindow.parse(window).keyFor(LocalDateTime.now(clock));

        return counters.consumed(new ConsumptionKey(subjectKind, subjectId, meter, windowKey));
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Refuses, at load, a rule asking about a meter or a window nothing writes.
     *
     * <h2>⚠️ This is the check that stops a quota quietly not existing</h2>
     *
     * <p>A counter row is keyed by its window, so {@code 3h} and {@code month} are different rows.
     * {@code consumed('ai-token', 'week')} where nothing records a {@code week} bucket reads
     * <strong>zero</strong> — every time, forever. The deny never holds, the limit is absent, and
     * nothing anywhere says so. It is the worst failure this whole mechanism can produce, and it is
     * caused by one mistyped word.
     *
     * <p>The window also has to <em>parse</em>. {@code '3 hours'} is not a window, and a rule carrying
     * one would throw on every decision — which, on a deny, is caught by the axis and applied, so
     * everybody is refused for a reason nobody can see. Better said now.
     *
     * <p>⚠️ Both arguments are skipped when unreadable, and so is everything when nothing was declared
     * as recorded. A check that cannot see the facts must not invent a verdict.
     */
    @Override
    public void verifyArguments(List<String> arguments) {
        String meter  = arguments.size() > 0 ? arguments.get(0) : null;
        String window = arguments.size() > 1 ? arguments.get(1) : null;

        if (window != null) {
            // Throws with its own sentence naming what would have worked.
            ConsumptionWindow.parse(window);
        }

        if (recorded.isEmpty() || meter == null) {
            return;
        }

        List<String> windows = recorded.get(meter);

        // ⚠️ The format arguments are applied to the WHOLE sentence, which is what the parentheses are
        // for. `"a" + "b".formatted(…)` binds the call to the last literal only, so every placeholder
        // before it survives into the message as a raw %s and the arguments meant for them are dropped
        // — silently, because String.format ignores arguments it was not asked for. The one message
        // whose entire job is to name what would have worked instead named nothing.
        if (windows == null) {
            throw new IllegalArgumentException(
                    ("nothing records a '%s' meter, so this would read zero forever and the rule would "
                     + "never hold. Recorded meters: %s.")
                            .formatted(meter, String.join(", ", recorded.keySet())));
        }

        if (window != null && !windows.contains(window)) {
            throw new IllegalArgumentException(
                    ("'%s' is recorded, but not in a '%s' window — so this would read zero forever and "
                     + "the rule would never hold. Recorded windows for '%s': %s. Add the window where "
                     + "the meter is written before asking about it here.")
                            .formatted(meter, window, meter, String.join(", ", windows)));
        }
    }

    /**
     * Whose usage this is, read from what the condition already bound.
     *
     * <p>⚠️ {@code ownedRowsBelongTo} is not used here even though an agent's <em>rows</em> belong to
     * its master. Consumption is about who is making calls, and an agent burning its master's quota
     * without appearing in it would make a runaway agent invisible in the one number that would have
     * shown it.
     */
    private static String callerId(EvaluationContext context) {
        return context.getValue(CALLER) instanceof CallerView caller ? caller.id() : null;
    }

    private static String required(Arguments arguments, int position, String name) {
        if (arguments.size() <= position) {
            throw new IllegalArgumentException(
                    "consumed(meter, window) needs a " + name + " — for example "
                    + "consumed('ai-token', '3h')");
        }

        return text(arguments.get(position));
    }

    private static String text(Object argument) {
        return argument == null ? null : String.valueOf(argument);
    }
}
