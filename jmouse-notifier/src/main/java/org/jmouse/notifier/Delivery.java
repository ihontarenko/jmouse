package org.jmouse.notifier;

import java.time.Duration;
import java.time.Instant;

/**
 * The state of one publication at one address.
 *
 * <p>Immutable: each attempt produces the next state through {@link #after(DeliveryOutcome, Instant, int)},
 * and the store keeps the latest.
 *
 * @param publicationId which publication
 * @param address       where
 * @param state         where it stands
 * @param attempts      how many times delivery was tried
 * @param receipt       the channel's handle on what it delivered, once {@link State#DELIVERED}
 * @param reason        the last sentence a refusal gave, or {@code null}
 * @param nextAttemptAt when a {@link State#PENDING} delivery is due, or {@code null} otherwise
 * @param updatedAt     when this state was reached
 */
public record Delivery(
        String publicationId,
        Address address,
        State state,
        int attempts,
        String receipt,
        String reason,
        Instant nextAttemptAt,
        Instant updatedAt
) {

    /** Where a delivery stands. */
    public enum State {

        /** Not delivered yet; due at {@code nextAttemptAt}. */
        PENDING,

        /** Arrived. Terminal. */
        DELIVERED,

        /** Will never arrive. Terminal — ⚠️ kept as a row, so "why did nothing arrive" has an answer. */
        FAILED
    }

    /** A delivery nobody has attempted yet, due now. */
    public static Delivery due(String publicationId, Address address, Instant now) {
        return new Delivery(publicationId, address, State.PENDING, 0, null, null, now, now);
    }

    /** A delivery that failed before any attempt — no channel by that name, for one. */
    public static Delivery failedUnattempted(String publicationId, Address address, String reason, Instant now) {
        return new Delivery(publicationId, address, State.FAILED, 0, null, reason, null, now);
    }

    /**
     * The state after one more attempt.
     *
     * @param maximumAttempts after this many, a retry becomes a failure — ⚠️ without a ceiling a
     *                        channel that always says "later" keeps a row due forever
     */
    public Delivery after(DeliveryOutcome outcome, Instant now, int maximumAttempts) {
        int attempted = attempts + 1;

        return switch (outcome) {
            case DeliveryOutcome.Delivered delivered ->
                    new Delivery(publicationId, address, State.DELIVERED, attempted, delivered.receipt(), null, null, now);
            case DeliveryOutcome.Failed failed ->
                    new Delivery(publicationId, address, State.FAILED, attempted, null, failed.reason(), null, now);
            case DeliveryOutcome.RetryAfter retry -> {
                if (attempted >= maximumAttempts) {
                    String reason = "gave up after %d attempts: %s".formatted(attempted, retry.reason());
                    yield new Delivery(publicationId, address, State.FAILED, attempted, null, reason, null, now);
                }

                Duration delay = retry.delay();
                yield new Delivery(publicationId, address, State.PENDING, attempted, null, retry.reason(), now.plus(delay), now);
            }
        };
    }

    public boolean isPending() {
        return state == State.PENDING;
    }

    public boolean isDueAt(Instant now) {
        return isPending() && !nextAttemptAt.isAfter(now);
    }
}
