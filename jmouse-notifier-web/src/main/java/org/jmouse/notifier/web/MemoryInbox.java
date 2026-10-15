package org.jmouse.notifier.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Screen notices in this process's memory — bounded, expiring, collected once per device.
 *
 * <p>⚠️ <strong>In memory on purpose.</strong> A restart losing uncollected notices loses nothing anybody
 * was waiting for, and a table would turn a nudge into a record — the notifier's own store already is the
 * record.
 */
public final class MemoryInbox implements Inbox {

    public static final int      DEFAULT_CAPACITY = 500;
    public static final Duration DEFAULT_LIFETIME = Duration.ofMinutes(10);

    /** Stands in for a caller that named no device. */
    private static final String UNNAMED_DEVICE = "";

    private final int                      capacity;
    private final Duration                 lifetime;
    private final Clock                    clock;
    private final Deque<Notice>            notices   = new ArrayDeque<>();
    private final Map<String, Set<String>> collected = new HashMap<>();

    public MemoryInbox() {
        this(DEFAULT_CAPACITY, DEFAULT_LIFETIME, Clock.systemUTC());
    }

    public MemoryInbox(int capacity, Duration lifetime, Clock clock) {
        if (capacity < 1 || lifetime == null || lifetime.isNegative() || lifetime.isZero()) {
            throw new IllegalArgumentException("An inbox needs room for one notice and a lifetime");
        }

        this.capacity = capacity;
        this.lifetime = lifetime;
        this.clock    = clock;
    }

    @Override
    public synchronized void deposit(Notice notice) {
        notices.addLast(notice);

        while (notices.size() > capacity) {
            collected.remove(notices.removeFirst().id());
        }
    }

    @Override
    public synchronized List<Notice> collect(String subject, String deviceId) {
        forgetExpired();

        String       device = deviceId == null ? UNNAMED_DEVICE : deviceId;
        List<Notice> fresh  = new ArrayList<>();

        for (Notice notice : notices) {
            if (notice.isFor(subject, deviceId) && collected.computeIfAbsent(notice.id(), ignored -> new HashSet<>()).add(device)) {
                fresh.add(notice);
            }
        }

        return fresh;
    }

    @Override
    public synchronized List<Notice> recent(String subject, int limit) {
        forgetExpired();

        List<Notice>         recent   = new ArrayList<>();
        Iterator<Notice>     newest   = notices.descendingIterator();

        while (newest.hasNext() && recent.size() < limit) {
            Notice notice = newest.next();

            if (notice.subject().equals(subject)) {
                recent.add(notice);
            }
        }

        return recent;
    }

    private void forgetExpired() {
        Instant oldestKept = clock.instant().minus(lifetime);

        while (!notices.isEmpty() && notices.peekFirst().at().isBefore(oldestKept)) {
            collected.remove(notices.removeFirst().id());
        }
    }
}
