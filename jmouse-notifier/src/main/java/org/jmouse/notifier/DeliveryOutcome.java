package org.jmouse.notifier;

import java.time.Duration;

/**
 * What one delivery attempt came to.
 *
 * <p>Sealed, so a {@code switch} over it breaks at compile time when a case is added.
 */
public sealed interface DeliveryOutcome {

    /**
     * It arrived.
     *
     * @param receipt the channel's handle on what it delivered — a chat and message id — or
     *                {@code null}. ⚠️ Stored, because it is the only way back to the message: editing
     *                one beats sending a fourth
     */
    record Delivered(String receipt) implements DeliveryOutcome {
    }

    /**
     * Not now; try again after exactly this long.
     *
     * <p>⚠️ When the far end named the delay (a flood wait), this is that delay, never a guessed backoff.
     *
     * @param delay  how long to wait
     * @param reason one sentence somebody can act on
     */
    record RetryAfter(Duration delay, String reason) implements DeliveryOutcome {

        public RetryAfter {
            if (delay == null || delay.isNegative()) {
                throw new IllegalArgumentException("A retry needs a delay that is not negative");
            }
        }
    }

    /**
     * Never going to arrive at this address — blocked, deleted, refused.
     *
     * @param reason one sentence somebody can act on
     */
    record Failed(String reason) implements DeliveryOutcome {
    }
}
