package org.jmouse.ai.guard;

import org.jmouse.ai.spi.DuplicateCallStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What each write produced, remembered in this process for as long as a retry could arrive.
 *
 * <p>The default. Behind more than one instance it suppresses only the duplicates that happen to land
 * on the same one, which is a partial protection rather than a broken one — and the direction it fails
 * in is the right one: the worst outcome is a second record, and refusing every write because a store
 * was unreachable would be far worse. A product that wants it complete implements
 * {@link DuplicateCallStore} over a shared cache.
 *
 * <p>Bounded, because a store with no bound is a leak with a window. Past {@link #capacity} the oldest
 * entries are discarded first, which for this data is exactly right — an entry old enough to be
 * evicted is one whose retry is no longer coming.
 */
public final class InMemoryDuplicateCallStore implements DuplicateCallStore {

    /** Enough to cover a busy window, small enough that the map cannot become the problem. */
    private static final int DEFAULT_CAPACITY = 10_000;

    private final Map<String, Remembered> remembered = new ConcurrentHashMap<>();
    private final Duration                window;
    private final int                     capacity;

    public InMemoryDuplicateCallStore(Duration window, int capacity) {
        this.window   = window;
        this.capacity = capacity;
    }

    public InMemoryDuplicateCallStore(Duration window) {
        this(window, DEFAULT_CAPACITY);
    }

    public InMemoryDuplicateCallStore() {
        this(GuardSettings.defaults().deduplicationWindow());
    }

    @Override
    public Optional<Object> findResult(String fingerprint) {
        Remembered previous = remembered.get(fingerprint);

        if (previous == null) {
            return Optional.empty();
        }

        if (previous.hasExpired()) {
            remembered.remove(fingerprint);
            return Optional.empty();
        }

        return Optional.ofNullable(previous.result());
    }

    @Override
    public void remember(String fingerprint, Object result) {
        discardExpired();

        if (remembered.size() >= capacity) {
            discardOldest();
        }

        remembered.put(fingerprint, new Remembered(result, Instant.now().plus(window)));
    }

    @Override
    public Duration window() {
        return window;
    }

    private void discardExpired() {
        remembered.values().removeIf(Remembered::hasExpired);
    }

    private void discardOldest() {
        remembered.entrySet().stream()
                .min(java.util.Map.Entry.comparingByValue(
                        java.util.Comparator.comparing(Remembered::expiresAt)))
                .map(Map.Entry::getKey)
                .ifPresent(remembered::remove);
    }

    private record Remembered(Object result, Instant expiresAt) {

        boolean hasExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
