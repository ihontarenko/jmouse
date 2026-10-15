package org.jmouse.notifier;

import java.util.List;

/**
 * What publishing came to.
 *
 * @param publication the publication that is kept — ⚠️ the earlier one when {@code repeated}
 * @param deliveries  its deliveries as they stand now
 * @param repeated    whether the idempotency key had been seen, so nothing was delivered this time
 */
public record PublicationReport(Publication publication, List<Delivery> deliveries, boolean repeated) {

    public PublicationReport {
        deliveries = List.copyOf(deliveries);
    }

    public long delivered() {
        return count(Delivery.State.DELIVERED);
    }

    public long pending() {
        return count(Delivery.State.PENDING);
    }

    public long failed() {
        return count(Delivery.State.FAILED);
    }

    /** Whether the notification reached nobody and nothing is left to try — worth a line in a log. */
    public boolean reachedNobody() {
        return delivered() == 0 && pending() == 0;
    }

    private long count(Delivery.State state) {
        return deliveries.stream().filter(delivery -> delivery.state() == state).count();
    }
}
