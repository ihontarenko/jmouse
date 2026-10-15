package org.jmouse.notifier.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;
import org.jmouse.notifier.Delivery;
import org.jmouse.notifier.NotificationStore;
import org.jmouse.notifier.Publication;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Publications and their delivery outbox in the database — {@code notifier_publications} and
 * {@code notifier_deliveries}. A pending retry survives a restart; an idempotency key survives it too.
 *
 * <h2>⚠️ Its own transaction, every time</h2>
 *
 * <p>Takes the {@link EntityManagerFactory} and commits a resource-local transaction per call, the way
 * {@code JpaTelegramJournal} does. A delivery has already happened when it is recorded — a message cannot
 * be un-sent — so the record must not roll back with a save that failed afterwards. The consequence a
 * product should know: <strong>publish after its own commit</strong> (an after-commit event listener),
 * or a rolled-back save will still have told somebody.
 *
 * <h2>⚠️ Several processes, one outbox — {@link #due} claims</h2>
 *
 * <p>Each row {@code due} returns is claimed first with a compare-and-set on {@code next_attempt_at}: the
 * row is pushed {@link #CLAIM} into the future only if nobody else moved it since it was read. Two
 * processes retrying at once therefore never attempt the same row. A process that dies holding a claim
 * releases it by expiry — the row is due again after {@link #CLAIM}.
 *
 * <h2>⚠️ At least once, not exactly once</h2>
 *
 * <p>A crash between a channel delivering and this store recording it leaves the row pending, and it will
 * be sent again. The idempotency key prevents a second <em>publication</em>, not a second attempt of one.
 * Exactly-once would need the channel's own cooperation, which Telegram does not offer.
 *
 * <p>⚠️ Needs a {@code RESOURCE_LOCAL} persistence unit — Spring Boot's default. Under JTA, replace it.
 */
public final class JpaNotificationStore implements NotificationStore {

    /** How long a claimed row is held before another process may take it. */
    public static final Duration CLAIM = Duration.ofMinutes(5);

    private final EntityManagerFactory entityManagerFactory;

    public JpaNotificationStore(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = Objects.requireNonNull(entityManagerFactory, "entity manager factory");
    }

    @Override
    public Publication register(Publication publication) {
        if (publication.hasIdempotencyKey()) {
            Optional<Publication> earlier = byKey(publication.idempotencyKey());

            if (earlier.isPresent()) {
                return earlier.get();
            }
        }

        try {
            inTransaction(entityManager -> {
                entityManager.persist(NotifierPublication.of(publication));
                return null;
            });
            return publication;
        } catch (PersistenceException exception) {
            // ⚠️ Two processes passed the lookup above at once; the unique key let one through. The
            // other answers with the winner, which is what a repeated key means.
            if (publication.hasIdempotencyKey()) {
                Optional<Publication> winner = byKey(publication.idempotencyKey());

                if (winner.isPresent()) {
                    return winner.get();
                }
            }

            throw exception;
        }
    }

    @Override
    public Optional<Publication> find(String publicationId) {
        return inTransaction(entityManager -> Optional
                .ofNullable(entityManager.find(NotifierPublication.class, publicationId))
                .map(NotifierPublication::toPublication));
    }

    @Override
    public void save(Delivery delivery) {
        inTransaction(entityManager -> {
            List<NotifierDelivery> existing = entityManager.createQuery("""
                            SELECT delivery FROM NotifierDelivery delivery
                            WHERE delivery.publicationId = :publication
                              AND delivery.channel = :channel AND delivery.address = :address
                            """, NotifierDelivery.class)
                    .setParameter("publication", delivery.publicationId())
                    .setParameter("channel", delivery.address().channel())
                    .setParameter("address", delivery.address().value())
                    .getResultList();

            if (existing.isEmpty()) {
                entityManager.persist(NotifierDelivery.of(delivery));
            } else {
                existing.getFirst().apply(delivery);
            }

            return null;
        });
    }

    @Override
    public List<Delivery> deliveriesOf(String publicationId) {
        return inTransaction(entityManager -> entityManager.createQuery("""
                        SELECT delivery FROM NotifierDelivery delivery
                        WHERE delivery.publicationId = :publication
                        ORDER BY delivery.updatedAt
                        """, NotifierDelivery.class)
                .setParameter("publication", publicationId)
                .getResultList()
                .stream()
                .map(NotifierDelivery::toDelivery)
                .toList());
    }

    @Override
    public List<Delivery> due(Instant now, int limit) {
        List<NotifierDelivery> candidates = inTransaction(entityManager -> entityManager.createQuery("""
                        SELECT delivery FROM NotifierDelivery delivery
                        WHERE delivery.state = :pending AND delivery.nextAttemptAt <= :now
                        ORDER BY delivery.nextAttemptAt
                        """, NotifierDelivery.class)
                .setParameter("pending", Delivery.State.PENDING)
                .setParameter("now", now)
                .setMaxResults(limit)
                .getResultList());

        List<Delivery> claimed = new ArrayList<>();
        Instant        lease   = now.plus(CLAIM);

        for (NotifierDelivery candidate : candidates) {
            int moved = inTransaction(entityManager -> entityManager.createQuery("""
                            UPDATE NotifierDelivery delivery SET delivery.nextAttemptAt = :lease
                            WHERE delivery.id = :id AND delivery.nextAttemptAt = :seen
                              AND delivery.state = :pending
                            """)
                    .setParameter("lease", lease)
                    .setParameter("id", candidate.getId())
                    .setParameter("seen", candidate.getNextAttemptAt())
                    .setParameter("pending", Delivery.State.PENDING)
                    .executeUpdate());

            if (moved == 1) {
                claimed.add(candidate.toDelivery());
            }
        }

        return claimed;
    }

    private Optional<Publication> byKey(String idempotencyKey) {
        return inTransaction(entityManager -> entityManager.createQuery("""
                        SELECT publication FROM NotifierPublication publication
                        WHERE publication.idempotencyKey = :key
                        """, NotifierPublication.class)
                .setParameter("key", idempotencyKey)
                .getResultStream()
                .findFirst()
                .map(NotifierPublication::toPublication));
    }

    private <T> T inTransaction(Function<EntityManager, T> work) {
        EntityManager     entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction   = entityManager.getTransaction();

        try {
            transaction.begin();
            T result = work.apply(entityManager);
            transaction.commit();
            return result;
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }

            throw exception;
        } finally {
            entityManager.close();
        }
    }
}
