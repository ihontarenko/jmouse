package org.jmouse.ai.view;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * How much has been used, and by whom.
 *
 * <p>Aggregated rather than walked: a trail answers <em>"what happened at 14:02"</em> and this answers
 * <em>"who is calling the delete action forty times a day"</em>, and the second question against a
 * million-row trail is a table scan somebody notices. Counters are cheap and are what a management
 * screen actually shows.
 *
 * <p>The grain is {@code (caller, action, outcome)} and the outcome belongs in the key rather than
 * being summed away. A caller whose calls are 90% {@code MISSING_PERMISSION} is the single most useful
 * thing this port says, and totalling by caller alone would hide exactly that.
 */
public interface UsageTotals {

    /**
     * One counted combination.
     *
     * @param callerId      who called
     * @param qualifiedName {@code tool.action}
     * @param outcome       the verdict or refusal reason these calls ended as
     * @param calls         how many
     * @param tokens        model tokens attributed to them, or zero where nothing attributes any —
     *                      token spend is a <em>conversation</em>-level number, and a product that
     *                      wants it here is the one that knows which conversation a call belonged to
     * @param lastCalledAt  when the most recent of them was
     */
    record Total(
            String  callerId,
            String  qualifiedName,
            String  outcome,
            long    calls,
            long    tokens,
            Instant lastCalledAt
    ) {
    }

    /** Every counted combination, busiest first. */
    List<Total> all();

    /** Everything one caller has done, busiest first. */
    default List<Total> forCaller(String callerId) {
        return all().stream()
                .filter(total -> total.callerId().equals(callerId))
                .sorted(Comparator.comparingLong(Total::calls).reversed())
                .toList();
    }

    /** Everything one action has been asked to do, busiest first. */
    default List<Total> forAction(String qualifiedName) {
        return all().stream()
                .filter(total -> total.qualifiedName().equals(qualifiedName))
                .sorted(Comparator.comparingLong(Total::calls).reversed())
                .toList();
    }

    /**
     * A product that counts nothing.
     *
     * <p>⚠️ As with {@link ToolCallHistory#none()}: empty here means "nothing is counting", which a
     * screen must not render as "nothing has happened".
     */
    static UsageTotals none() {
        return List::of;
    }
}
