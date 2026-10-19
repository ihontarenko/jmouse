package org.jmouse.parallax.circuit;

import java.util.Map;
import java.util.TreeMap;

/**
 * What one digital output drives: its level now, the one before and when it changed — so a bridge can ramp between
 * them — and the transactions still scheduled on it.
 *
 * <p>⚠️ <strong>Transport, not inertial, delay.</strong> A new transaction removes only those scheduled at or after
 * its own time. Firmware writes "on now, off in a moment" in one breath; inertial cancellation would swallow the
 * first half of that.</p>
 */
final class DigitalDriver {

    final String unit;
    final String pin;
    final String digitalNode;

    private LogicLevel level = LogicLevel.RELEASED;
    private LogicLevel previous = LogicLevel.RELEASED;
    private double changedAt = Double.NEGATIVE_INFINITY;
    private final TreeMap<Double, LogicLevel> pending = new TreeMap<>();

    DigitalDriver(String unit, String pin, String digitalNode) {
        this.unit = unit;
        this.pin = pin;
        this.digitalNode = digitalNode;
    }

    LogicLevel level() {
        return level;
    }

    LogicLevel previous() {
        return previous;
    }

    /** Whether a transaction is still scheduled on it. */
    boolean hasPending() {
        return !pending.isEmpty();
    }

    /** When the first transaction after {@code time} is due; null for none. */
    Double dueAfter(double time) {
        return pending.higherKey(time);
    }

    /** When it last changed level; minus infinity while it never has. */
    double changedAt() {
        return changedAt;
    }

    void schedule(double time, LogicLevel value) {
        pending.tailMap(time, true).clear();
        pending.put(time, value);
    }

    /** Applies every transaction due by {@code time}; true when the level changed. */
    boolean applyDue(double time) {
        boolean changed = false;
        while (!pending.isEmpty() && pending.firstKey() <= time) {
            Map.Entry<Double, LogicLevel> transaction = pending.pollFirstEntry();
            if (transaction.getValue() != level) {
                previous = level;
                level = transaction.getValue();
                changedAt = transaction.getKey();
                changed = true;
            }
        }
        return changed;
    }

    /** Takes over where {@code earlier} — the same output in an earlier build of the circuit — stands. */
    void continueFrom(DigitalDriver earlier) {
        level = earlier.level;
        previous = earlier.previous;
        changedAt = earlier.changedAt;
        pending.clear();
        pending.putAll(earlier.pending);
    }
}
