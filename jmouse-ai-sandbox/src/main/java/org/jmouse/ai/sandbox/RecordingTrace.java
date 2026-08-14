package org.jmouse.ai.sandbox;

import org.jmouse.ai.CallerIdentity;
import org.jmouse.ai.InvocationScope;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolRefusedException;
import org.jmouse.ai.guard.GuardedCall;
import org.jmouse.ai.spi.InvocationTrace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Everything that happened, in a list, so the driver can read back what was recorded.
 *
 * <p>Composes what a real product would spread across an audit trail, a counter and an activity stamp
 * behind one implementation — which is the arrangement {@link InvocationTrace} exists to make possible,
 * and this is the smallest thing that demonstrates it.
 *
 * <p>Reads {@code traceAttributes} and never asks the library what they mean, which is the other half:
 * a product's vocabulary travels through the mechanism untouched.
 */
public final class RecordingTrace implements InvocationTrace {

    /**
     * @param outcome a verdict name, {@code FAILED}, or the name of the reason it was refused — one
     *                axis, because a total refusal count says something is wrong and only the
     *                distribution says what
     */
    public record Entry(String caller, String scope, String action, String outcome, String detail) {
    }

    /** What a caller that reached no action at all is counted against. */
    public static final String NO_ACTION = "-";

    /** A call that was attempted and threw — counted, but never as a refusal. */
    public static final String FAILED = "FAILED";

    private final List<Entry> entries = new ArrayList<>();

    @Override
    public void recordOutcome(
            CallerIdentity caller, InvocationScope scope, ToolAction action, GuardedCall guarded) {

        entries.add(new Entry(caller.describe(), scopeOf(scope), action.qualifiedName(),
                guarded.verdict().name(), describeWhatItReached(action, guarded)));
    }

    @Override
    public void recordRefusal(
            CallerIdentity caller, InvocationScope scope, ToolAction action,
            ToolRefusedException refusal) {

        entries.add(new Entry(caller.describe(), scopeOf(scope), action.qualifiedName(),
                refusal.reason().name(), refusal.getMessage()));
    }

    @Override
    public void recordFailure(
            CallerIdentity caller, InvocationScope scope, ToolAction action, RuntimeException failure) {

        entries.add(new Entry(caller.describe(), scopeOf(scope), action.qualifiedName(),
                FAILED, failure.getClass().getSimpleName()));
    }

    @Override
    public void recordUnknownAction(CallerIdentity caller, String publishedName) {
        entries.add(new Entry(caller.describe(), NO_ACTION, NO_ACTION,
                "UNKNOWN_ACTION", "asked for '" + publishedName + "'"));
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    /** How many of each outcome — the breakdown an operator actually reads. */
    public Map<String, Long> outcomeTotals() {
        return entries.stream().collect(Collectors.groupingBy(
                Entry::outcome, java.util.TreeMap::new, Collectors.counting()));
    }

    /**
     * The product's own vocabulary, plus what the call reached.
     *
     * <p>Only previous state that went through confirmation is kept, and only where the work actually
     * happened: a preview carries the same snapshot and changed nothing, so writing it would keep a
     * copy of records that still exist — and a second copy when the confirming call arrives.
     */
    private String describeWhatItReached(ToolAction action, GuardedCall guarded) {
        String recorded = action.traceAttributes().getOrDefault("event", "(not recorded)");

        if (guarded.affectedCount() == 0) {
            return recorded;
        }

        boolean keepPreviousState = guarded.throughConfirmation() && guarded.verdict().changedSomething();

        return recorded + " — " + guarded.affectedCount() + " record(s)"
             + (keepPreviousState ? ", previous state kept for " + guarded.affected().size() : "");
    }

    private String scopeOf(InvocationScope scope) {
        return scope == null ? NO_ACTION : scope.label();
    }
}
