package org.jmouse.notifier.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jmouse.notifier.Address;
import org.jmouse.notifier.Delivery;

import java.time.Instant;
import java.util.UUID;

/**
 * The state of one publication at one address — the outbox row.
 *
 * <p>One row per (publication, channel, address), replaced in place on every attempt.
 */
@Entity
@Table(name = NotifierDelivery.TABLE_NAME)
public class NotifierDelivery {

    public static final String TABLE_NAME = "notifier_deliveries";

    static final int REASON_LIMIT = 1000;

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "publication_id", nullable = false, length = 36)
    private String publicationId;

    @Column(name = "channel", nullable = false, length = 64)
    private String channel;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private Delivery.State state;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    /** The channel's handle on what it delivered — a chat and message id. */
    @Column(name = "receipt", length = 255)
    private String receipt;

    @Column(name = "reason", length = REASON_LIMIT)
    private String reason;

    /** When a pending row is due — and, while claimed, when the claim lapses. */
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotifierDelivery() {
        // Jakarta Persistence.
    }

    static NotifierDelivery of(Delivery delivery) {
        NotifierDelivery row = new NotifierDelivery();
        row.id            = UUID.randomUUID().toString();
        row.publicationId = delivery.publicationId();
        row.channel       = delivery.address().channel();
        row.address       = delivery.address().value();
        row.apply(delivery);
        return row;
    }

    void apply(Delivery delivery) {
        state         = delivery.state();
        attempts      = delivery.attempts();
        receipt       = delivery.receipt();
        reason        = clipped(delivery.reason());
        nextAttemptAt = delivery.nextAttemptAt();
        updatedAt     = delivery.updatedAt();
    }

    Delivery toDelivery() {
        return new Delivery(publicationId, new Address(channel, address), state, attempts, receipt, reason,
                            nextAttemptAt, updatedAt);
    }

    String getId() {
        return id;
    }

    Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    private static String clipped(String value) {
        if (value == null || value.length() <= REASON_LIMIT) {
            return value;
        }

        return value.substring(0, REASON_LIMIT - 1) + "…";
    }
}
