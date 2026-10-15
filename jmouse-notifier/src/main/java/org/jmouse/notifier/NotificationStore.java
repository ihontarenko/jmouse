package org.jmouse.notifier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Where publications and their delivery state are kept.
 *
 * <p>{@link InMemoryNotificationStore} is the one here. ⚠️ It forgets everything on a restart, pending
 * retries included — a database-backed store that survives one is a separate module, and choosing
 * memory is choosing that a notice nobody received before a restart is not worth sending after it.
 */
public interface NotificationStore {

    /**
     * Keeps a publication — unless one with the same idempotency key is already kept.
     *
     * @return the publication that is now kept: the argument, or ⚠️ <strong>the earlier one</strong>
     *         when the key was seen before. The {@link Notifier} compares identifiers to tell which
     */
    Publication register(Publication publication);

    Optional<Publication> find(String publicationId);

    /** Keeps a delivery's latest state, replacing the previous one for the same publication and address. */
    void save(Delivery delivery);

    /** Every delivery of one publication, in the order they were first saved. */
    List<Delivery> deliveriesOf(String publicationId);

    /** Pending deliveries due at or before {@code now}, earliest first, at most {@code limit}. */
    List<Delivery> due(Instant now, int limit);
}
