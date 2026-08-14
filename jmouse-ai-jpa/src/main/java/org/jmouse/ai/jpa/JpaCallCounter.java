package org.jmouse.ai.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.ai.CallerIdentity;
import org.jmouse.ai.InvocationScope;
import org.jmouse.ai.ToolAction;
import org.jmouse.ai.ToolRefusedException;
import org.jmouse.ai.guard.GuardedCall;
import org.jmouse.ai.jpa.entity.AiToolCall;
import org.jmouse.ai.spi.InvocationTrace;
import org.jmouse.ai.view.UsageTotals;
import org.jmouse.core.SecureRandomStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Counts every call, and — when one is refused — what refused it.
 *
 * <p>The part of a trail every adopter wants and wants identically, which is why it is the library's
 * while the audit trail is not. A product that records a tool call and a human action as the
 * <em>same</em> event, so that "everything created this week" stays one query, is making a decision this
 * library must not make for it; a count of how often {@code parts.discard} was refused for over-ceiling
 * is not a decision at all.
 *
 * <p><strong>Compose rather than choose.</strong> This is an {@link InvocationTrace}, and so is a
 * product's audit recorder; a product that wants both writes a third that calls both. That is the
 * arrangement the interface exists to make possible.
 *
 * <h2>Its own transaction, and why that is not a detail</h2>
 *
 * <p>⚠️ A counter that joined the caller's transaction would be rolled back exactly when the call
 * failed — which is when the count matters most. Metrics that quietly under-report failures are worse
 * than none, because they are believed.
 *
 * <p>And it can never fail the work. Every one of these methods swallows its own exception with a
 * warning: a call that did what it was asked must not be reported as broken because a counter row
 * could not be written.
 */
public final class JpaCallCounter implements InvocationTrace, UsageTotals {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaCallCounter.class);

    /**
     * A call that was attempted and threw.
     *
     * <p>Counted, but never as a refusal: a refusal is a decision this mechanism made on purpose, and
     * mixing the two puts "something is broken" in the middle of the breakdown an operator reads to
     * find out which guard is firing.
     */
    public static final String FAILED = "FAILED";

    /** Stands in for the tool of a call that never reached one. */
    public static final String CONNECTION_TOOL = "connection";

    /** Stands in for the action of the same. */
    public static final String ACCESS_ACTION = "access";

    private static final int IDENTIFIER_LENGTH = 36;

    private final EntityManagerFactory        entityManagerFactory;
    private final SecureRandomStringGenerator identifiers =
            new SecureRandomStringGenerator(IDENTIFIER_LENGTH);

    public JpaCallCounter(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void recordOutcome(
            CallerIdentity caller, InvocationScope scope, ToolAction action, GuardedCall guarded) {

        count(caller.callerId(), action.toolName(), action.name(), guarded.verdict().name());
    }

    @Override
    public void recordRefusal(
            CallerIdentity caller, InvocationScope scope, ToolAction action, ToolRefusedException refusal) {

        count(caller.callerId(), action.toolName(), action.name(), refusal.reason().name());
    }

    @Override
    public void recordFailure(
            CallerIdentity caller, InvocationScope scope, ToolAction action, RuntimeException failure) {

        count(caller.callerId(), action.toolName(), action.name(), FAILED);
    }

    /**
     * A name that matched nothing, counted against the caller rather than against an action.
     *
     * <p>⚠️ The name asked for is <strong>not</strong> recorded as the action, deliberately. It arrives
     * from outside and is unbounded — a model inventing names would write a new row per invention — so
     * what is counted is that this caller asked for something that is not there. The name itself belongs
     * in a log line, where volume is somebody else's problem.
     */
    @Override
    public void recordUnknownAction(CallerIdentity caller, String publishedName) {
        LOGGER.info("{} asked for '{}', which does not exist", caller.describe(), publishedName);

        count(caller.callerId(), CONNECTION_TOOL, ACCESS_ACTION, "UNKNOWN_ACTION");
    }

    /**
     * A call refused before any action was reached — a credential this installation will not accept, a
     * switched-off caller, a feature that is off.
     *
     * <p>Public because those refusals happen in a product's {@code CallerResolver}, above everything
     * this library sees, and they are exactly the ones an operator asks about first.
     */
    public void countAccessRefusal(String callerId, String reason) {
        count(callerId, CONNECTION_TOOL, ACCESS_ACTION, reason);
    }

    /** What has been counted so far, most-called first. For a management screen, and nothing else. */
    public List<CallCount> totals() {
        return OwnTransaction.call(entityManagerFactory, entityManager -> entityManager.createQuery("""
                        select new org.jmouse.ai.jpa.CallCount(
                                counted.callerId, counted.toolName, counted.actionName,
                                counted.outcome, counted.callCount, counted.lastCalledAt)
                          from AiToolCall counted
                         order by counted.callCount desc
                        """, CallCount.class)
                .getResultList());
    }

    /**
     * The same counts, as the read port a management screen talks to.
     *
     * <p>⚠️ Every {@link Total#tokens()} is zero, and that is the truth rather than a gap left to fill
     * later. These rows count <em>calls</em>; token spend is a conversation-level number that only
     * whatever ran the conversation knows how to attribute to a caller, and inventing a column for it
     * here would produce a screen confidently reporting zero tokens for an application spending them.
     * A product that meters tokens implements {@link UsageTotals} over its own rows.
     *
     * <p>⚠️ The instant is composed with the system zone, and that is symmetry rather than an omission:
     * {@code last_called_at} is a zoneless column written from {@link LocalDateTime#now()}, so the zone
     * the row was written in is the same one it is read back in. Reading it as UTC would be the version
     * that quietly shifts every timestamp on a machine that is not.
     */
    @Override
    public List<Total> all() {
        return totals().stream()
                .map(counted -> new Total(
                        counted.callerId(),
                        counted.qualifiedName(),
                        counted.outcome(),
                        counted.callCount(),
                        0,
                        counted.lastCalledAt().atZone(ZoneId.systemDefault()).toInstant()))
                .toList();
    }

    // ── One count ────────────────────────────────────────────────────────────────

    /**
     * @param outcome a verdict name, {@link #FAILED}, or the name of the reason it was refused — one
     *                column, because the distribution is what an operator actually reads
     */
    public void count(String callerId, String toolName, String actionName, String outcome) {
        try {
            OwnTransaction.run(entityManagerFactory,
                    entityManager -> increment(entityManager, callerId, toolName, actionName, outcome));

        } catch (RuntimeException uncounted) {
            // ⚠️ Swallowed on purpose. Two callers counting the same key at the same instant is the
            // ordinary case for this, and losing one count is not worth failing a call that worked.
            LOGGER.warn("Could not count {}.{} ({}) for {}: {}",
                    toolName, actionName, outcome, callerId, uncounted.getMessage());
        }
    }

    /**
     * Increment first, insert only if there was nothing to increment.
     *
     * <p>The other order would read then write, and read-then-write is where two threads both find
     * nothing and both insert. This way the race is narrowed to the first ever call for one key, where
     * the loser's insert collides, the transaction fails, and exactly one count is lost.
     */
    private void increment(
            EntityManager entityManager,
            String callerId, String toolName, String actionName, String outcome) {

        int updated = entityManager.createQuery("""
                        update AiToolCall counted
                           set counted.callCount    = counted.callCount + 1,
                               counted.lastCalledAt = :now
                         where counted.callerId   = :callerId
                           and counted.toolName   = :toolName
                           and counted.actionName = :actionName
                           and counted.outcome    = :outcome
                        """)
                .setParameter("now",        LocalDateTime.now())
                .setParameter("callerId",   callerId)
                .setParameter("toolName",   toolName)
                .setParameter("actionName", actionName)
                .setParameter("outcome",    outcome)
                .executeUpdate();

        if (updated == 0) {
            entityManager.persist(new AiToolCall(
                    identifiers.generate(), callerId, toolName, actionName, outcome,
                    1, LocalDateTime.now()));
        }
    }
}
