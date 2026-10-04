package org.jmouse.script.event;

import java.util.Optional;

/**
 * Something a product is <em>about</em> to do, offered to anybody who would be broken by it.
 *
 * <h2>⚠️ Why refusal is state on the event and not a thrown exception</h2>
 *
 * <p>A walk processes thousands of items, and an exception raised on one of them unwinds the walk. That
 * is the difference between "this one is not for us" and "the walk failed", and a mechanism that
 * expressed both the same way would eventually turn somebody's rule into a job that dies at item 400
 * with 3 600 unlooked-at.</p>
 *
 * <p>So a listener <em>marks</em> the event, the publisher <em>asks</em>, and the refusal never escapes
 * into a loop that was not written to catch it:</p>
 *
 * <pre>{@code
 * ItemSeen seen = new ItemSeen(rootId, relativePath, name);
 *
 * dispatcher.decide(seen);
 *
 * if (seen.isRefused()) {
 *     continue;
 * }
 * }</pre>
 *
 * <h2>⚠️ Published BEFORE the consequence, always</h2>
 *
 * <p>A decision event and the observation that follows it are two events, never one object with a flag.
 * They carry opposite guarantees — a decision must arrive in time to prevent something, an observation
 * must survive its listener failing — and one type holding both would quietly give each of them the
 * weaker half.</p>
 *
 * <h2>⚠️ The first refusal wins</h2>
 *
 * <p>Later listeners still run: a publisher generally offers no way to stop propagation, and pretending
 * otherwise would make behaviour depend on listener order. They may read {@link #isRefused()} and stand
 * down. What they may not do is overwrite an answer already given — a reader who is shown one reason and
 * finds another in the log is a reader who stops believing the log.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class DecisionEvent implements DomainEvent {

    private Refusal refusal;

    /**
     * Refuses what is about to happen, naming the refuser and the reason.
     *
     * <p>⚠️ Ignored when something has already refused. See the class note.</p>
     *
     * @param by     what refused — a rule's name, or a guard's class
     * @param reason a phrase a person can read
     */
    public void refuse(String by, String reason) {
        if (refusal == null) {
            refusal = new Refusal(by, reason);
        }
    }

    /**
     * Whether anybody refused.
     *
     * @return true when this must not go ahead
     */
    public boolean isRefused() {
        return refusal != null;
    }

    /**
     * Who refused and why.
     *
     * @return the refusal, or empty when nobody objected
     */
    public Optional<Refusal> getRefusal() {
        return Optional.ofNullable(refusal);
    }

    /**
     * What the refusal reads as, for a log line.
     *
     * @return the phrase, or {@code null} when nobody refused
     */
    public String refusalDescription() {
        return refusal == null ? null : refusal.describe();
    }

}
