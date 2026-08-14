package org.jmouse.ai.spi;

import org.jmouse.ai.guard.PendingConfirmation;

import java.time.Duration;
import java.util.Optional;

/**
 * Where a pending preview is held between the call that produced it and the call that confirms it.
 *
 * <p>Short-lived, single-use material — the same shape as a one-time authorization code — which is why
 * this is a seam rather than a table: a product with a cache already has the right place for it, and
 * one without should not be made to prune rows for something that lives five minutes.
 *
 * <p>⚠️ <strong>Consuming is destructive whether or not the checks that follow then pass.</strong> A
 * token that has been presented once is never worth presenting again, and a store that returned it
 * without spending it would let a caller grind through the five validation refusals learning what the
 * preview was for.
 *
 * <p>⚠️ Unlike {@link DuplicateCallStore}, an implementation of this should <strong>fail
 * closed</strong>. If the store cannot be read, the honest answer is that the confirmation is not
 * valid: letting a destructive call through because the store was unavailable is precisely the
 * outcome two-step confirmation exists to prevent.
 */
public interface ConfirmationStore {

    /** Holds a preview and returns the token that redeems it. */
    String issue(PendingConfirmation pending);

    /** Spends a token, if it exists. Empty means unknown, expired, or already spent. */
    Optional<PendingConfirmation> consume(String token);

    /** How long a freshly issued token lasts, for the preview that has to say so. */
    Duration lifetime();
}
