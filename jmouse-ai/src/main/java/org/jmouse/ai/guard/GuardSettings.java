package org.jmouse.ai.guard;

import java.time.Duration;

/**
 * The four numbers the guards work from.
 *
 * <p>Settings rather than constants, deliberately. <strong>What counts as "too many records" depends
 * entirely on the installation</strong>: a workshop inventory of eighty parts and a catalogue of nine
 * thousand want different answers, and neither should need a release to say so. Binding them from
 * configuration is the starter's business; this is the shape they arrive in, so
 * {@code jmouse-ai} needs no property mechanism of its own.
 *
 * @param perCallCeiling        the most records one call may affect. Past this the call is
 *                              <strong>refused</strong>, not confirmed — no amount of agreeing should
 *                              produce an operation of that size
 * @param confirmationThreshold above how many affected records a non-destructive write still needs
 *                              confirming. A bulk update is not a deletion, but zeroing five hundred
 *                              quantities is the same disaster without the word
 * @param confirmationLifetime  how long a preview's token stays redeemable — long enough to read the
 *                              preview, not long enough to forget what it was for
 * @param deduplicationWindow   how long an identical call is treated as the same call. Sized for a
 *                              transport retry or a model that did not see the response, not for
 *                              somebody genuinely adding two of something
 */
public record GuardSettings(
        long     perCallCeiling,
        long     confirmationThreshold,
        Duration confirmationLifetime,
        Duration deduplicationWindow
) {

    /**
     * What a product gets before it has thought about any of this.
     *
     * <p>Chosen to be annoying rather than permissive. A ceiling that is too low produces a refusal
     * naming the number, which somebody raises in a minute; one that is too high produces a call
     * nobody notices until the records are gone.
     */
    public static GuardSettings defaults() {
        return new GuardSettings(100, 5, Duration.ofMinutes(5), Duration.ofSeconds(60));
    }

    public GuardSettings withPerCallCeiling(long ceiling) {
        return new GuardSettings(ceiling, confirmationThreshold, confirmationLifetime, deduplicationWindow);
    }

    public GuardSettings withConfirmationThreshold(long threshold) {
        return new GuardSettings(perCallCeiling, threshold, confirmationLifetime, deduplicationWindow);
    }
}
