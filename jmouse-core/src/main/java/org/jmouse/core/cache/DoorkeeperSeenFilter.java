package org.jmouse.core.cache;

/**
 * 🧩 Adapter turning {@link Doorkeeper} into a {@link SeenFilter}.
 *
 * <p>Bridges the TinyLFU admission API with the two-bit
 * Doorkeeper filter:</p>
 * <ul>
 *   <li>🚪 {@link #pass(KeyWrapper)} → delegates to {@link Doorkeeper#admit(Object)}.</li>
 *   <li>♻️ {@link #reset()} → clears Doorkeeper bits (start fresh).</li>
 * </ul>
 *
 * @param <K> key type
 */
public final class DoorkeeperSeenFilter<K> implements SeenFilter<K> {

    private final Doorkeeper<K> keeper;

    /**
     * 🏗️ Create a new wrapper.
     *
     * @param keeper underlying Doorkeeper instance
     */
    public DoorkeeperSeenFilter(Doorkeeper<K> keeper) {
        this.keeper = keeper;
    }

    /**
     * 🚪 Test whether a key passes the Doorkeeper.
     *
     * <p>First sighting → sets bits, returns {@code false} (blocked).
     * Second sighting → bits already set, returns {@code true} (passed).</p>
     *
     * @param key wrapped key
     * @return {@code true} if key already seen (passes to CMS),
     *         {@code false} on first sighting
     */
    @Override
    public boolean pass(KeyWrapper<K> key) {
        return keeper.admit(key.value());
    }

    /**
     * ♻️ Reset the Doorkeeper (clear all bits).
     *
     * <p>Forces all keys to require two fresh sightings again.</p>
     */
    @Override
    public void reset() {
        keeper.reset();
    }
}
