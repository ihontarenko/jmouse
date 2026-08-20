package org.jmouse.access.jpa;

import org.jmouse.access.ConsumptionKey;

/**
 * Adding to a counter — the write half, kept away from the engine on purpose.
 *
 * <p>The same split {@code EntitlementAdministration} makes against {@code EntitlementStore}, and for
 * the same reason: the engine gets an interface it can only read through, so no amount of convenience
 * later turns a decision into a write. Recording is the product's act, made after its own transaction
 * committed, from the code that knows what was actually consumed.
 *
 * <h2>⚠️ A window is created by being written to</h2>
 *
 * <p>There is no "open a period" call and no reset job. {@link ConsumptionKey#windowKey()} carries which
 * period this is, so August and September are different rows and neither has to be prepared. A reset
 * job that failed to run would hand somebody an unlimited month and the failure would surface as an
 * invoice rather than as an alert.
 *
 * <h2>⚠️ Must be atomic</h2>
 *
 * <p>Two requests recording against one counter at the same moment must add to it, not overwrite each
 * other with a read-modify-write. An implementation that cannot express that as one statement should
 * lock the row rather than hope — a quota that undercounts under load is a quota that stops existing
 * exactly when it is needed.
 */
public interface ConsumptionRecording {

    /**
     * Adds to what one subject has used, creating the counter where the window is new.
     *
     * @param key    what is counted, for whom, when
     * @param amount how much to add; must not be negative — a correction is a product's own concern and
     *               a negative here would let a caller quietly refund itself past a limit
     * @return the amount consumed in that window after this call
     */
    long record(ConsumptionKey key, long amount);
}
