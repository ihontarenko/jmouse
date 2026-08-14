package org.jmouse.ai.spi;

import java.time.Duration;
import java.util.Optional;

/**
 * What each write produced, briefly, so the same write arriving twice does not happen twice.
 *
 * <p>Keyed by a hash of the call itself rather than by an idempotency key the client supplies. That
 * pattern assumes a caller deliberately managing keys across retries, which is exactly the kind of
 * bookkeeping a language model does unreliably — and a scheme that works only when the caller
 * remembers to use it protects nobody.
 *
 * <p>⚠️ <strong>An implementation should fail open.</strong> If the store cannot be reached, letting
 * the call through produces at worst a duplicate record; refusing produces an application that cannot
 * be written to at all because a cache is down. The ceiling and the rate limit are still standing
 * either way. This is the opposite of {@link ConfirmationStore}'s advice, and the difference is what
 * the failure costs.
 */
public interface DuplicateCallStore {

    /** What an identical call produced within the window, if one did. */
    Optional<Object> findResult(String fingerprint);

    /** Remembers what this call produced, for as long as a retry could plausibly arrive. */
    void remember(String fingerprint, Object result);

    /** How long the window is, for the message that has to say so. */
    Duration window();
}
