package org.jmouse.scheduler.store;

import org.jmouse.scheduler.OccurrenceRecord;
import org.jmouse.scheduler.Trigger;
import org.jmouse.scheduler.TriggerId;
import org.jmouse.scheduler.spi.ScheduledTrigger;
import org.jmouse.scheduler.spi.TriggerLease;
import org.jmouse.scheduler.spi.TriggerStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link TriggerStore} in this process's memory — for tests, and for an application that accepts
 * losing its triggers on restart. Claims are atomic across threads, so several runners over one
 * instance never hold the same lease; it does nothing for two processes.
 *
 * <p>Leases are measured on the store's own {@link Clock}, which a test can move independently of the
 * runners' clock.
 */
public final class InMemoryTriggerStore implements TriggerStore {

    private final Map<TriggerId, Entry> entries = new ConcurrentHashMap<>();
    private final Map<TriggerId, Deque<OccurrenceRecord>> histories = new ConcurrentHashMap<>();
    private final int historyLimit;
    private final Clock storeClock;

    /**
     * @param historyLimit the most history records kept per trigger, at least one
     */
    public InMemoryTriggerStore(int historyLimit) {
        this(historyLimit, Clock.systemUTC());
    }

    /**
     * @param historyLimit the most history records kept per trigger, at least one
     * @param storeClock   the clock leases are measured on
     */
    public InMemoryTriggerStore(int historyLimit, Clock storeClock) {
        if (historyLimit < 1) {
            throw new IllegalArgumentException("The history limit must be at least one, not " + historyLimit + ".");
        }

        this.historyLimit = historyLimit;
        this.storeClock = Objects.requireNonNull(storeClock, "storeClock");
    }

    @Override
    public ScheduledTrigger save(Trigger trigger, Instant nextFireAt) {
        Objects.requireNonNull(trigger, "trigger");
        return entries.compute(trigger.id(), (id, existing) ->
                new Entry(trigger, nextFireAt, existing == null ? 1 : existing.version() + 1, null, null)).scheduled();
    }

    @Override
    public Optional<ScheduledTrigger> find(TriggerId id) {
        return Optional.ofNullable(entries.get(id)).map(Entry::scheduled);
    }

    @Override
    public List<ScheduledTrigger> findAll() {
        List<ScheduledTrigger> all = new ArrayList<>();

        for (Entry entry : entries.values()) {
            all.add(entry.scheduled());
        }

        all.sort(Comparator.comparing(scheduled -> scheduled.trigger().id().value()));
        return all;
    }

    @Override
    public boolean remove(TriggerId id) {
        histories.remove(id);
        return entries.remove(id) != null;
    }

    @Override
    public List<ScheduledTrigger> findDue(Instant now, int limit) {
        Instant storeNow = storeClock.instant();
        return entries.values().stream()
                .filter(entry -> entry.trigger().enabled())
                .filter(entry -> entry.nextFireAt() != null && !entry.nextFireAt().isAfter(now))
                .filter(entry -> !entry.leased(storeNow))
                .map(Entry::scheduled)
                .sorted(Comparator.comparing(ScheduledTrigger::nextFireAt))
                .limit(limit)
                .toList();
    }

    @Override
    public Optional<TriggerLease> claim(TriggerId id, long expectedVersion, Instant plannedAt, Duration leaseDuration) {
        Objects.requireNonNull(plannedAt, "plannedAt");
        AtomicReference<TriggerLease> lease = new AtomicReference<>();
        Instant storeNow = storeClock.instant();

        entries.computeIfPresent(id, (key, existing) -> {
            if (existing.version() != expectedVersion || existing.leased(storeNow)) {
                return existing;
            }

            long version = existing.version() + 1;
            lease.set(new TriggerLease(id, version, plannedAt));
            return new Entry(existing.trigger(), existing.nextFireAt(), version, plannedAt, storeNow.plus(leaseDuration));
        });

        return Optional.ofNullable(lease.get());
    }

    @Override
    public boolean confirm(TriggerLease lease, Instant nextFireAt) {
        AtomicBoolean confirmed = new AtomicBoolean(false);

        entries.computeIfPresent(lease.triggerId(), (key, existing) -> {
            if (existing.version() != lease.version()) {
                return existing;
            }

            confirmed.set(true);
            return new Entry(existing.trigger(), nextFireAt, existing.version() + 1, null, null);
        });

        return confirmed.get();
    }

    @Override
    public void record(OccurrenceRecord occurrenceRecord) {
        Deque<OccurrenceRecord> history = histories.computeIfAbsent(occurrenceRecord.triggerId(),
                id -> new ArrayDeque<>());

        synchronized (history) {
            history.addFirst(occurrenceRecord);

            while (history.size() > historyLimit) {
                history.removeLast();
            }
        }
    }

    @Override
    public List<OccurrenceRecord> history(TriggerId id, int limit) {
        Deque<OccurrenceRecord> history = histories.get(id);

        if (history == null) {
            return List.of();
        }

        List<OccurrenceRecord> newestFirst = new ArrayList<>();

        synchronized (history) {
            Iterator<OccurrenceRecord> iterator = history.iterator();

            while (iterator.hasNext() && newestFirst.size() < limit) {
                newestFirst.add(iterator.next());
            }
        }

        return newestFirst;
    }

    private record Entry(Trigger trigger, Instant nextFireAt, long version, Instant plannedAt, Instant leaseExpiresAt) {

        boolean leased(Instant storeNow) {
            return leaseExpiresAt != null && leaseExpiresAt.isAfter(storeNow);
        }

        ScheduledTrigger scheduled() {
            return new ScheduledTrigger(trigger, nextFireAt, version, plannedAt);
        }
    }
}
