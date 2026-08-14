package org.jmouse.ai.sandbox;

import org.jmouse.ai.CallerIdentity;
import org.jmouse.ai.InvocationScope;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolRefusedException;
import org.jmouse.ai.guard.GuardedCall;
import org.jmouse.ai.spi.InvocationTrace;
import org.jmouse.ai.view.ToolCallHistory;
import org.jmouse.ai.view.UsageTotals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
 *
 * <p><strong>And it answers the two read ports off the same rows.</strong> {@link ToolCallHistory} and
 * {@link UsageTotals} are deliberately separate interfaces from the writing half, so that a product
 * recording tool calls into a trail of its own shape can still be asked <em>"what has been called"</em>
 * and <em>"how much"</em> — without the library having decided how any of it is stored. This class is
 * what that looks like: one list, written once, read three ways.
 */
public final class RecordingTrace implements InvocationTrace, ToolCallHistory, UsageTotals {

    /**
     * @param outcome a verdict name, {@code FAILED}, or the name of the reason it was refused — one
     *                axis, because a total refusal count says something is wrong and only the
     *                distribution says what
     */
    public record Entry(
            String  operationId,
            String  callerId,
            String  actingSubject,
            String  caller,
            String  scope,
            String  scopeId,
            String  action,
            String  outcome,
            long    affectedCount,
            Instant at,
            String  detail
    ) {
    }

    /** What a caller that reached no action at all is counted against. */
    public static final String NO_ACTION = "-";

    /** A call that was attempted and threw — counted, but never as a refusal. */
    public static final String FAILED = "FAILED";

    private final List<Entry> entries = new ArrayList<>();

    @Override
    public void recordOutcome(
            CallerIdentity caller, InvocationScope scope, ToolAction action, GuardedCall guarded) {

        record(guarded.operationId(), caller, scope, action.qualifiedName(),
                guarded.verdict().name(), guarded.affectedCount(),
                describeWhatItReached(action, guarded));
    }

    @Override
    public void recordRefusal(
            CallerIdentity caller, InvocationScope scope, ToolAction action,
            ToolRefusedException refusal) {

        record(null, caller, scope, action.qualifiedName(),
                refusal.reason().name(), 0, refusal.getMessage());
    }

    @Override
    public void recordFailure(
            CallerIdentity caller, InvocationScope scope, ToolAction action, RuntimeException failure) {

        record(null, caller, scope, action.qualifiedName(),
                FAILED, 0, failure.getClass().getSimpleName());
    }

    @Override
    public void recordUnknownAction(CallerIdentity caller, String publishedName) {
        record(null, caller, null, NO_ACTION,
                "UNKNOWN_ACTION", 0, "asked for '" + publishedName + "'");
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    /** How many of each outcome — the breakdown an operator actually reads. */
    public Map<String, Long> outcomeTotals() {
        return entries.stream().collect(Collectors.groupingBy(
                Entry::outcome, java.util.TreeMap::new, Collectors.counting()));
    }

    // ── What a management screen reads ───────────────────────────────────────────

    @Override
    public List<ToolCallHistory.Entry> recent(int limit) {
        return newestFirst(entries.stream(), limit);
    }

    @Override
    public List<ToolCallHistory.Entry> forAction(String qualifiedName, int limit) {
        return newestFirst(
                entries.stream().filter(entry -> entry.action().equals(qualifiedName)), limit);
    }

    @Override
    public List<ToolCallHistory.Entry> forCaller(String callerId, int limit) {
        return newestFirst(
                entries.stream().filter(entry -> entry.callerId().equals(callerId)), limit);
    }

    /**
     * Counted by caller, action and outcome — the grain that makes this worth opening.
     *
     * <p>⚠️ Every token figure is zero, and that is the truth rather than a gap. These rows count
     * <em>calls</em>; token spend is a conversation-level number, and only whatever ran the conversation
     * knows which caller to attribute it to.
     */
    @Override
    public List<Total> all() {
        return entries.stream()
                .collect(Collectors.groupingBy(CountedBy::of))
                .values().stream()
                .map(RecordingTrace::totalOf)
                .sorted(Comparator.comparingLong(Total::calls).reversed())
                .toList();
    }

    /**
     * The grain the counts are kept at.
     *
     * <p>Named rather than a {@code List.of(…)} used as a map key, because the grain is the whole
     * argument for this port: the outcome stays in the key instead of being summed away, and a caller
     * whose calls are mostly {@code MISSING_PERMISSION} is the single most useful thing a usage screen
     * reports. A positional list would have said that in a place nobody reads.
     */
    private record CountedBy(String callerId, String action, String outcome) {

        static CountedBy of(Entry entry) {
            return new CountedBy(entry.callerId(), entry.action(), entry.outcome());
        }
    }

    // ── One row ──────────────────────────────────────────────────────────────────

    private void record(
            String         operationId,
            CallerIdentity caller,
            InvocationScope scope,
            String         action,
            String         outcome,
            long           affectedCount,
            String         detail) {

        entries.add(new Entry(
                operationId, caller.callerId(), caller.actingSubject(), caller.describe(),
                scopeOf(scope), InvocationScope.identifierOf(scope),
                action, outcome, affectedCount, Instant.now(), detail));
    }

    private static List<ToolCallHistory.Entry> newestFirst(
            java.util.stream.Stream<Entry> selected, int limit) {

        List<Entry> ordered = selected
                .sorted(Comparator.comparing(Entry::at).reversed())
                .limit(Math.max(limit, 0))
                .toList();

        return ordered.stream().map(RecordingTrace::asHistoryEntry).toList();
    }

    private static ToolCallHistory.Entry asHistoryEntry(Entry entry) {
        return new ToolCallHistory.Entry(
                entry.operationId(), entry.callerId(), entry.actingSubject(), entry.action(),
                entry.scopeId(), entry.scope(), entry.outcome(), entry.affectedCount(), entry.at());
    }

    private static Total totalOf(List<Entry> counted) {
        Entry first = counted.getFirst();

        return new Total(
                first.callerId(), first.action(), first.outcome(), counted.size(), 0,
                counted.stream().map(Entry::at).max(Comparator.naturalOrder()).orElseThrow());
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
