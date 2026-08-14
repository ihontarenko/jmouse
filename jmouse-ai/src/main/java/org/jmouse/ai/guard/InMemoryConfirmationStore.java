package org.jmouse.ai.guard;

import org.jmouse.ai.spi.ConfirmationStore;
import org.jmouse.core.SecureRandomStringGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pending previews held in this process, expiring on their own.
 *
 * <p>The default, and honest about what it is: a token issued here is redeemable only by whichever
 * instance issued it, and a restart forgets every one. For a single instance that is exactly right and
 * costs nothing; behind more than one it means a confirmation occasionally has to be previewed again,
 * which is a mild annoyance rather than a correctness problem — the token that cannot be found is
 * refused, and refusing is the safe direction.
 *
 * <p>A product that cannot live with that implements {@link ConfirmationStore} over whatever it keeps
 * short-lived material in. Nothing else changes.
 *
 * <p>Expiry is checked on read rather than swept on a timer, because a store this size does not need
 * a thread and a sweeper is a thing to shut down. Entries a caller never comes back for are cleared
 * opportunistically on the next issue, so an abandoned preview cannot accumulate indefinitely.
 */
public final class InMemoryConfirmationStore implements ConfirmationStore {

    /** Long enough that a token is not guessable, short enough that a model will echo it back intact. */
    private static final int TOKEN_LENGTH = 32;

    private final Map<String, Held>           held = new ConcurrentHashMap<>();
    private final SecureRandomStringGenerator tokens;
    private final Duration                    lifetime;

    public InMemoryConfirmationStore(Duration lifetime) {
        this.lifetime = lifetime;
        this.tokens   = new SecureRandomStringGenerator(TOKEN_LENGTH);
    }

    public InMemoryConfirmationStore() {
        this(GuardSettings.defaults().confirmationLifetime());
    }

    @Override
    public String issue(PendingConfirmation pending) {
        discardExpired();

        String token = tokens.generate();
        held.put(token, new Held(pending, Instant.now().plus(lifetime)));

        return token;
    }

    @Override
    public Optional<PendingConfirmation> consume(String token) {
        // Removed whatever happens next: a token that has been presented once is never worth
        // presenting again, and leaving it behind would let a caller grind through the five
        // validation refusals learning what the preview was for.
        Held taken = held.remove(token);

        if (taken == null || taken.hasExpired()) {
            return Optional.empty();
        }

        return Optional.of(taken.pending());
    }

    @Override
    public Duration lifetime() {
        return lifetime;
    }

    /** How many previews are outstanding — for a sandbox or a diagnostic, not for a decision. */
    public int outstanding() {
        discardExpired();
        return held.size();
    }

    private void discardExpired() {
        held.values().removeIf(Held::hasExpired);
    }

    private record Held(PendingConfirmation pending, Instant expiresAt) {

        boolean hasExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
