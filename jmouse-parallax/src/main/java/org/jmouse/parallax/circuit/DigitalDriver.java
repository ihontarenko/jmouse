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

    /** The levels a digital signal takes. A number 0 or 1, or one of these. */
    static final String UNKNOWN = "X";
    static final String RELEASED = "Z";

    final String unit;
    final String pin;
    final String digitalNode;

    Object level = RELEASED;
    Object previous = RELEASED;
    double changedAt = Double.NEGATIVE_INFINITY;
    final TreeMap<Double, Object> pending = new TreeMap<>();

    DigitalDriver(String unit, String pin, String digitalNode) {
        this.unit = unit;
        this.pin = pin;
        this.digitalNode = digitalNode;
    }

    void schedule(double time, Object value) {
        pending.tailMap(time, true).clear();
        pending.put(time, value);
    }

    /** Applies every transaction due by {@code time}; true when the level changed. */
    boolean applyDue(double time) {
        boolean changed = false;
        while (!pending.isEmpty() && pending.firstKey() <= time) {
            Map.Entry<Double, Object> transaction = pending.pollFirstEntry();
            if (!transaction.getValue().equals(level)) {
                previous = level;
                level = transaction.getValue();
                changedAt = transaction.getKey();
                changed = true;
            }
        }
        return changed;
    }

    /** A level written by a script: anything at or above ½ is 1, below is 0, and X and Z stay themselves. */
    static Object normalized(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue() >= 0.5 ? 1 : 0;
        }
        if (value instanceof Boolean flag) {
            return flag ? 1 : 0;
        }
        String text = String.valueOf(value);
        return text.equalsIgnoreCase(RELEASED) ? RELEASED : UNKNOWN;
    }

    /** The level as a fraction of the supply: 0, 1, or ½ for an unknown one. */
    static double fraction(Object level) {
        return level instanceof Integer integer ? integer : 0.5;
    }
}
