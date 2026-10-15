package org.jmouse.notifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Publications and their deliveries, in this process's memory.
 *
 * <h2>⚠️ Bounded, and the oldest goes first</h2>
 *
 * <p>Only the most recent {@code capacity} publications are kept. When one more arrives, the oldest is
 * forgotten <em>with</em> its deliveries and its idempotency key — including a delivery still pending.
 * That is the trade memory makes: a store that grew without limit would be a leak, and a notification
 * a thousand publications old is not one anybody is waiting for.
 *
 * <h2>⚠️ Per process</h2>
 *
 * <p>Two instances of a product do not share it, so an idempotency key protects against a repeat within
 * one process only.
 */
public final class InMemoryNotificationStore implements NotificationStore {

    /** What is kept when nothing says otherwise. */
    public static final int DEFAULT_CAPACITY = 1000;

    private final int                                  capacity;
    private final LinkedHashMap<String, Publication>   publications = new LinkedHashMap<>();
    private final Map<String, String>                  idempotencyKeys = new HashMap<>();
    private final Map<String, Map<Address, Delivery>>  deliveries = new HashMap<>();

    public InMemoryNotificationStore() {
        this(DEFAULT_CAPACITY);
    }

    public InMemoryNotificationStore(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("A store has to keep at least one publication");
        }

        this.capacity = capacity;
    }

    @Override
    public synchronized Publication register(Publication publication) {
        if (publication.hasIdempotencyKey()) {
            String earlierId = idempotencyKeys.get(publication.idempotencyKey());

            if (earlierId != null) {
                return publications.get(earlierId);
            }

            idempotencyKeys.put(publication.idempotencyKey(), publication.id());
        }

        publications.put(publication.id(), publication);
        forgetBeyondCapacity();

        return publication;
    }

    @Override
    public synchronized Optional<Publication> find(String publicationId) {
        return Optional.ofNullable(publications.get(publicationId));
    }

    @Override
    public synchronized void save(Delivery delivery) {
        if (!publications.containsKey(delivery.publicationId())) {
            // Forgotten already — a late result for a publication pushed out by newer ones.
            return;
        }

        deliveries.computeIfAbsent(delivery.publicationId(), ignored -> new LinkedHashMap<>())
                  .put(delivery.address(), delivery);
    }

    @Override
    public synchronized List<Delivery> deliveriesOf(String publicationId) {
        Map<Address, Delivery> kept = deliveries.get(publicationId);

        if (kept == null) {
            return List.of();
        }

        return List.copyOf(kept.values());
    }

    @Override
    public synchronized List<Delivery> due(Instant now, int limit) {
        List<Delivery> due = new ArrayList<>();

        for (Map<Address, Delivery> perPublication : deliveries.values()) {
            for (Delivery delivery : perPublication.values()) {
                if (delivery.isDueAt(now)) {
                    due.add(delivery);
                }
            }
        }

        due.sort(Comparator.comparing(Delivery::nextAttemptAt));

        return due.size() > limit ? List.copyOf(due.subList(0, limit)) : List.copyOf(due);
    }

    private void forgetBeyondCapacity() {
        while (publications.size() > capacity) {
            Publication oldest = publications.pollFirstEntry().getValue();
            deliveries.remove(oldest.id());

            if (oldest.hasIdempotencyKey()) {
                idempotencyKeys.remove(oldest.idempotencyKey());
            }
        }
    }
}
