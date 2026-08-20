package org.jmouse.access.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.access.ConsumptionKey;
import org.jmouse.access.spi.ConsumptionCounters;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Counters out of the engine's own table — both halves, behind two interfaces.
 *
 * <p>It implements the read {@link ConsumptionCounters} and the write {@link ConsumptionRecording}
 * separately so that whoever is handed one cannot reach the other. One class because they are one
 * table; two interfaces because a decision may read and must not write.
 *
 * <h2>⚠️ The increment is a statement, not a read-modify-write</h2>
 *
 * <p>Two requests recording against the same counter in the same instant must both land. Loading the
 * row, adding in Java and saving it loses one of them under exactly the load a quota exists for — and
 * loses it silently, because both callers see a plausible number.
 *
 * <p>So the update is {@code SET consumed = consumed + :amount} evaluated by the database, and the row
 * is only inserted when that update matched nothing. ⚠️ Two callers can still race to insert the
 * <em>first</em> row of a window; the unique key on
 * {@code (subject_type, subject_id, meter, window_key)} is what makes the loser fail rather than create
 * a second counter, and the retry then takes the update path.
 */
public class JpaConsumptionCounters implements ConsumptionCounters, ConsumptionRecording {

    private final EntityManager    entityManager;
    private final Supplier<String> idGenerator;

    public JpaConsumptionCounters(EntityManager entityManager) {
        this(entityManager, () -> UUID.randomUUID().toString());
    }

    public JpaConsumptionCounters(EntityManager entityManager, Supplier<String> idGenerator) {
        this.entityManager = entityManager;
        this.idGenerator   = idGenerator;
    }

    @Override
    public long consumed(ConsumptionKey key) {
        List<Long> found = entityManager.createQuery("""
                        SELECT counter.consumed FROM AccessConsumptionCounter counter
                         WHERE counter.subjectType = :subjectType
                           AND counter.subjectId   = :subjectId
                           AND counter.meter       = :meter
                           AND counter.windowKey   = :windowKey
                        """, Long.class)
                .setParameter("subjectType", key.subjectKind())
                .setParameter("subjectId",   key.subjectId())
                .setParameter("meter",       key.meter())
                .setParameter("windowKey",   key.windowKey())
                .getResultList();

        // A window nobody has written to is a window nobody has used, not a missing row to complain
        // about — otherwise the first request of every period fails.
        return found.isEmpty() ? 0L : found.get(0);
    }

    @Override
    public long record(ConsumptionKey key, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException(
                    "a consumption of " + amount + " would refund a caller past its own limit; "
                    + "correcting a counter is the product's own business, not this one's");
        }

        if (amount == 0) {
            return consumed(key);
        }

        int updated = entityManager.createQuery("""
                        UPDATE AccessConsumptionCounter counter
                           SET counter.consumed  = counter.consumed + :amount,
                               counter.updatedAt = :now
                         WHERE counter.subjectType = :subjectType
                           AND counter.subjectId   = :subjectId
                           AND counter.meter       = :meter
                           AND counter.windowKey   = :windowKey
                        """)
                .setParameter("amount",      amount)
                .setParameter("now",         Instant.now())
                .setParameter("subjectType", key.subjectKind())
                .setParameter("subjectId",   key.subjectId())
                .setParameter("meter",       key.meter())
                .setParameter("windowKey",   key.windowKey())
                .executeUpdate();

        if (updated == 0) {
            entityManager.persist(new org.jmouse.access.jpa.entity.AccessConsumptionCounter(
                    idGenerator.get(), key.subjectKind(), key.subjectId(), key.meter(),
                    key.windowKey(), amount, Instant.now()));

            return amount;
        }

        // ⚠️ Read back rather than returned from arithmetic here: the increment happened in the
        // database, possibly alongside somebody else's, so the only honest total is the one it holds.
        entityManager.clear();

        return consumed(key);
    }
}
