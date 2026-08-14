package org.jmouse.ai.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.ai.AffectedRecords;
import org.jmouse.ai.guard.PendingConfirmation;
import org.jmouse.ai.jpa.entity.AiPendingConfirmation;
import org.jmouse.ai.spi.ConfirmationStore;
import org.jmouse.core.SecureRandomStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Previews that outlive the process that issued them.
 *
 * <p>For a product with more than one instance and no shared cache. The in-memory store shipped with
 * the guards is correct for a single process and wrong the moment there are two: a token issued by one
 * instance and redeemed against another is refused as unknown, and the person who did exactly what they
 * were told is informed that their confirmation expired.
 *
 * <h2>Its own transaction, for the opposite reason to the counter's</h2>
 *
 * <p>⚠️ A token is spent whether or not the work that redeemed it succeeds. Deliberately: a token
 * un-spent by a rollback is a token that can be redeemed a second time, and "the operation failed,
 * preview it again" is a far better outcome than a confirmation that quietly still works. The cost is
 * that a failed confirmation has to be previewed again — which is the safe direction, and the preview
 * will show what is actually there now rather than what was there before the failure.
 *
 * @see ConfirmationStore
 */
public final class JpaConfirmationStore implements ConfirmationStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaConfirmationStore.class);

    /** Long enough that guessing is not a strategy; it is a bearer credential for one operation. */
    private static final int TOKEN_LENGTH = 32;

    private static final TypeReference<List<AffectedRecords.Record>> RECORDS = new TypeReference<>() {
    };

    private final EntityManagerFactory        entityManagerFactory;
    private final Duration                    lifetime;
    private final ObjectMapper                json = new ObjectMapper();
    private final SecureRandomStringGenerator tokens = new SecureRandomStringGenerator(TOKEN_LENGTH);

    public JpaConfirmationStore(EntityManagerFactory entityManagerFactory, Duration lifetime) {
        this.entityManagerFactory = entityManagerFactory;
        this.lifetime             = lifetime;
    }

    @Override
    public String issue(PendingConfirmation pending) {
        String token = tokens.generate();

        OwnTransaction.run(entityManagerFactory, entityManager -> entityManager.persist(
                new AiPendingConfirmation(
                        token,
                        pending.operationId(),
                        pending.callerId(),
                        pending.publishedName(),
                        pending.fingerprint(),
                        pending.scopeId(),
                        writeRecords(pending.records()),
                        Instant.now().plus(lifetime))));

        return token;
    }

    /**
     * Spends a token, if it is there and still redeemable.
     *
     * <p>⚠️ <strong>Removed, not read.</strong> A store that returned the preview and left the row in
     * place would let a confirmation be replayed, and nothing above this interface is in a position to
     * notice. An expired row is removed too — it is of no use to anyone, and leaving it would make the
     * sweep the only thing keeping this table bounded.
     */
    @Override
    public Optional<PendingConfirmation> consume(String token) {
        return Optional.ofNullable(OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiPendingConfirmation held = entityManager.find(AiPendingConfirmation.class, token);

            if (held == null) {
                return null;
            }

            entityManager.remove(held);

            return held.hasExpired(Instant.now()) ? null : restore(held);
        }));
    }

    @Override
    public Duration lifetime() {
        return lifetime;
    }

    /**
     * Removes what nobody came back for.
     *
     * <p>Every redemption already removes its own row, so this only ever collects previews somebody
     * looked at and thought better of — which is a legitimate and common thing to do, and the reason
     * this table needs a sweep at all. Called by whatever a product already uses to schedule work;
     * this library has no scheduler and should not grow one.
     *
     * @return how many were removed
     */
    public int sweepExpired() {
        int removed = OwnTransaction.call(entityManagerFactory, entityManager -> entityManager
                .createQuery("delete from AiPendingConfirmation held where held.expiresAt < :now")
                .setParameter("now", Instant.now())
                .executeUpdate());

        if (removed > 0) {
            LOGGER.debug("Swept {} expired confirmation(s)", removed);
        }

        return removed;
    }

    // ── The frozen set, across a serialisation boundary ──────────────────────────

    private String writeRecords(List<AffectedRecords.Record> records) {
        try {
            return json.writeValueAsString(records);
        } catch (Exception unwritable) {
            // Nothing can be done with a preview whose records did not survive being written, and
            // issuing a token against an empty set would confirm nothing and then destroy something.
            throw new IllegalStateException(
                    "A preview's affected records could not be stored: " + unwritable.getMessage(),
                    unwritable);
        }
    }

    private PendingConfirmation restore(AiPendingConfirmation held) {
        try {
            return new PendingConfirmation(
                    held.getOperationId(),
                    held.getCallerId(),
                    held.getPublishedName(),
                    held.getFingerprint(),
                    held.getScopeId(),
                    json.readValue(held.getRecords(), RECORDS));

        } catch (Exception unreadable) {
            throw new IllegalStateException(
                    "A stored preview could not be read back: " + unreadable.getMessage(), unreadable);
        }
    }
}
