package org.jmouse.parallax.circuit;

/**
 * How far one Newton iteration may move the voltage that controls a device — the rule SPICE gives each device for
 * its own curve. Newton linearizes the device at the voltage this returns instead of the one it asked for: a step
 * that would put a volt across a junction is taken on the logarithm of its current, so it cannot ask for an
 * astronomical one, and the next iteration starts from a point the curve can be trusted at.
 *
 * <p>An implementation knows its device's curve; the device keeps the last limited voltage between iterations and
 * hands it back as {@code previous}.</p>
 */
interface VoltageLimit {

    /**
     * The voltage to linearize at.
     *
     * @param wanted   the voltage this iteration's guess puts across the device
     * @param previous the voltage it was linearized at last iteration; NaN before the first
     */
    double limited(double wanted, double previous);
}
